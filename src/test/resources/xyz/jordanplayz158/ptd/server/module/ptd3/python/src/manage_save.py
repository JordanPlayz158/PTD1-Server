from uuid import uuid4
from urllib.parse import unquote

from werkzeug.datastructures import ImmutableMultiDict

from src.models.networking import Request

from src.models.extras_2 import ExtraInfoLoader, ExtraInfo
from src.models.pokemon import PokeLoader, Pokemon
from src.models.items import ItemsLoader, Item
from src.models.profile import ProfileLoader, Profile

from src.database import Database, Query, Field

profile_base = Database("ptd3_profiles_database", record_type=Profile)

extra_base = Database("ptd3_extra_info_database", record_type=ExtraInfo)
poke_base = Database("ptd3_pokemon_database", record_type=Pokemon)
item_base = Database("ptd3_item_database", record_type=Item)


def get_profiles_list(data: ImmutableMultiDict[str, str]) -> dict:
    result = {}
    username = data.get("Account")
    if username is None:
        username = unquote(data['Email']).rsplit('/', 1)[-1]

    profiles: list[Profile] = profile_base.fetch(Query(Field("key").startswith(username)))
    data = ProfileLoader.encode_profiles(profiles, result)
    result['extra'] = data
    return result


def get_story_profile(data: ImmutableMultiDict[str, str]) -> dict:
    username = data.get("Account")
    if username is None:
        username = unquote(data['Email']).rsplit('/', 1)[-1]
    profile_key = f"{username}${data['whichProfile']}"

    profile = profile_base.get(profile_key)
    extras = extra_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)
    pokemons = poke_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)
    items = item_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)
    result = dict()
    result['extra'] = ProfileLoader.encode_story_profile(profile, result)
    result['extra2'] = ExtraInfoLoader.encode_story_extras(extras, result)
    result['extra3'] = PokeLoader.encode_story_pokemons(pokemons, result)
    result['extra4'] = ItemsLoader.encode_story_items(items, result)
    # extra5 and CS are added by the caller
    return result


def set_save_data(data: ImmutableMultiDict[str, str]) -> dict:
    "Saves the data and returns the new pokemons IDs"
    profile_meta = {
        outer_key: part
        for outer_key, part in (x.split('=') for x in unquote(data["extra"]).split('&'))
    }
    username = data.get("Account")
    if username is None:
        username = unquote(data['Email']).rsplit('/', 1)[-1]
    profile_key = f"{username}${data['whichProfile']}"

    profile = profile_base.get(profile_key)
    saved_pokemons = poke_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)
    profile: Profile = ProfileLoader.update_profile(profile, profile_meta)
    profile.profile_id = data['whichProfile']  # not part of the proper `extra` metadata

    _poke_nicks = {int(key.removeprefix('PokeNick')): value for key, value in profile_meta.items() if
                   key.startswith('PokeNick')}

    extras = ExtraInfoLoader(data=data['extra2'], infos=[])
    pokemons = PokeLoader(data=data['extra3'], pokemons=saved_pokemons, poke_nicks=_poke_nicks)
    items = ItemsLoader(data=data['extra4'], items=[])
    # extra5 => current save key ; only used for the checkSum, which we ignore

    extras.load()
    pokemons.load()
    items.load()

    profile_base[profile_key] = profile

    extra_base.put_many(
        extras.infos,
        key_source=lambda record: f"{profile_key}${record.info_id}",
    )

    poke_base.put_many(
        pokemons.to_insert + pokemons.to_update,
        key_source=lambda record: f"{profile_key}${record.poke_save_id}",
        iter=True,
    )
    for poke in pokemons.to_delete:
        poke_base.delete(f"{profile_key}${poke.poke_save_id}")
        # Keep a backup to permit manual recovery
        poke_base.put(f"backup_save$0${uuid4()}", poke, expire_in=7 * 24 * 60 * 60)

    item_base.put_many(
        items.items,
        key_source=lambda record: f"{profile_key}${record.item_id}",
        iter=True,
    )

    result = {}
    for pokemon in pokemons.to_insert:  # Return the newly created Save IDs
        result[f'PID{pokemon.poke_party_pos}'] = pokemon.poke_save_id
    return result


def delete_save_data(request: Request) -> dict:
    "Deletes the profile related save data"
    username = request.data.get("Account")
    if username is None:
        username = unquote(request.data['Email']).rsplit('/', 1)[-1]
    profile_key = f"{username}${request.data['whichProfile']}"

    saved_pokemons = poke_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)
    saved_extras = extra_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)
    saved_items = item_base.fetch(Query(Field("key").startswith(profile_key)), follow_last=True)

    del profile_base[profile_key]

    for poke in saved_pokemons:
        poke_base.delete(f"{profile_key}${poke.poke_save_id}")
        # Keep a backup to permit manual recovery
        poke_base.put(f"backup_save$0${uuid4()}", poke, expire_in=7 * 24 * 60 * 60)

    for extra in saved_extras:
        extra_base.delete(f"{profile_key}${extra.info_id}")

    for item in saved_items:
        item_base.delete(f"{profile_key}${item.item_id}")
