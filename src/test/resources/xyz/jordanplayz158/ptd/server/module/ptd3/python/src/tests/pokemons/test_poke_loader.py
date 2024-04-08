import pytest
from src.models.pokemon import Pokemon, PokeLoader

def test_first_save(first_party):
    "Tests loading new pokemons for the first time"
    loader = PokeLoader(
        data="woeywyyyymyycyrwycyyrypwwwcqapycymyyyyymymymynyyymymymyyyymyywyoywyeyqyyycyqymyyyyyyymymynycymymym",
        pokemons=[],
        poke_nicks={1: 'Pichu', 2: 'Rattata'}
    )
    loader.load()
    assert loader.to_insert[0] == first_party['pichu']
    assert loader.to_insert[1] == first_party['rattata']
    assert len(loader.to_update) == 0
    assert len(loader.to_delete) == 0

def test_second_save(first_party, second_party):
    "Tests loading new pokemons when other pokemons already exist, as well as updating existing pokemons"
    loader = PokeLoader(
        data="weaycycyyyycycywyywyrwyyycywyywycywqwywypyyyymyywrqyymyqyywwaymymyyywywymymynywymymym",
        pokemons=list(first_party.values()),
        poke_nicks={3: 'Geodude'}
    )
    loader.load()
    assert loader.to_insert[0] == second_party['geodude']
    assert loader.to_update[0] == second_party['pichu']
    assert loader.to_update[1] == second_party['rattata']
    assert len(loader.to_delete) == 0

@pytest.mark.skip("test not implemented")
def test_release(first_party):
    "Tests releasing existing pokemons"
    loader = PokeLoader(
        data="...",
        pokemons=list(first_party.values()),
    )
    loader.load()
    assert loader.to_delete == ...

def test_encode(first_party):
    "Tests encoding the saved data to feed back to the game client"
    url_params = {}
    data = PokeLoader.encode_story_pokemons(
        pokemons=list(first_party.values()),
        url_parameters=url_params,
    )
    assert url_params['PN1'] == 'Pichu'
    assert url_params['PN2'] == 'Rattata'
    assert data == 'woqywcyrwycywyyrwwwcqapycymyyyyyyyymymymynymycymymymwyoywqwypyyycyqymyyyyyywyyymymynymycymymym'
