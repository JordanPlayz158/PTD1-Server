from dataclasses import dataclass
from typing import Optional

from src.models.extractor import DataExtractor, encode
from src.database import Record

@dataclass
class Pokemon(Record):
    poke_save_id: int
    pokedex_num: int
    poke_exp: int
    poke_lvl: int
    move_1_id: int
    move_2_id: int
    move_3_id: int
    move_4_id: int
    targetting_type: int
    poke_gender: int
    poke_party_pos: int
    poke_extra: int  # shiny/shadow/elemental
    poke_held_item: int
    poke_is_hacked_tag: str  # hacked = 'h', normal = 'n'
    poke_selected_move: int
    poke_selected_ability: int
    poke_nickname: str


class PokeLoader(DataExtractor):
    "Loads pokemons from `saveStory` -> `extra3`"
    def __init__(self, data: str, pokemons: list[Pokemon], *, poke_nicks: Optional[dict] = None) -> None:
        super().__init__(data)
        self.pokemons = pokemons
        self.poke_nicks = poke_nicks or {}
        
        self.to_insert: list[Pokemon] = []
        self.to_update: list[Pokemon] = []
        self.to_delete: list[Pokemon] = []

        if pokemons:
            self._last_id = max(poke.poke_save_id for poke in pokemons) + 1
        else:
            self._last_id = 1

    def load(self):
        final_index = self.get_number().value
        number_of_pokes = self.get_number().value
        for poke_index in range(1, number_of_pokes+1):
            number_of_fields = self.get_number().value
            poke_save_id = self.get_double_number().value

            if poke_save_id == 0:  # New pokemon
                poke = None
                poke_save_id = self._last_id
                self._last_id += 1
            else:  # Update or delete (release) existing pokemon
                poke = next(pk for pk in self.pokemons if pk.poke_save_id == poke_save_id)

            for field in range(number_of_fields):
                field_id = self.get_number().value
                if field_id == 1: # New pokemon - Full data
                    assert poke is None
                    poke = Pokemon(
                        poke_save_id = poke_save_id,
                        pokedex_num = (pokedex := self.get_number().value),
                        poke_exp = self.get_double_number().value,
                        poke_lvl = self.get_number().value,
                        move_1_id = self.get_number().value,
                        move_2_id = self.get_number().value,
                        move_3_id = self.get_number().value,
                        move_4_id = self.get_number().value,
                        targetting_type = self.get_number().value,
                        poke_gender = self.get_number().value,
                        poke_party_pos = self.get_number().value,
                        poke_extra = 0 if (element := self.get_number().value) == 0 else (element - pokedex),  # elemental, technically also shiny/shadow
                        poke_held_item = self.get_number().value,
                        poke_is_hacked_tag = self.get_string(),
                        poke_selected_move = self.get_number().value,
                        poke_selected_ability = self.get_number().value,
                        poke_nickname=self.poke_nicks.get(poke_index),
                    )
                    unused_1 = self.get_number()  # must leave here for the current_index offset
                    unused_2 = self.get_number()  # these two unused should always be zero btw
                    self.to_insert.append(poke)
                elif field_id == 15:  # Release - Must Delete
                    assert poke is not None
                    self.to_delete.append(poke)
                else: # To update
                    assert poke is not None
                    if poke not in self.to_update:
                        self.to_update.append(poke)

                    if field_id == 3:  # Update Experience
                        poke.poke_exp = self.get_double_number().value
                    elif field_id == 2:  # Update level
                        poke.poke_lvl = self.get_number().value
                    elif field_id == 4:  # Update moves
                        poke.move_1_id = self.get_number().value
                        poke.move_2_id = self.get_number().value
                        poke.move_3_id = self.get_number().value
                        poke.move_4_id = self.get_number().value
                    elif field_id == 5:  # Update held item
                        poke.poke_held_item = self.get_number().value
                    elif field_id == 6:  # Update Evolution (well, pokedex num)
                        poke.pokedex_num = self.get_number().value
                    elif field_id == 10:  # Update Trade...wut? leaving more or less as-is based on the client *shrug*
                        poke.pokedex_num = self.get_number().value
                    elif field_id == 7:  # Update Nickname
                        poke.poke_nickname = self.poke_nicks[poke_index]
                    elif field_id == 8:  # Update Position
                        poke.poke_party_pos = self.get_number().value
                    elif field_id == 9:  # Update (hacked) Tag
                        poke.poke_is_hacked_tag = self.get_string()
                    elif field_id == 11:  # Update selected Move
                        poke.poke_selected_move = self.get_number().value
                    elif field_id == 12:  # Update selected Ability
                        poke.poke_selected_ability = self.get_number().value
                    elif field_id == 13:  # Update <UNUSED>. Not sure if this would ever be triggered but whatever
                        unused_1 = self.get_number()
                    elif field_id == 14:  # Update <UNUSED>. Not sure if this would ever be triggered but whatever
                        unused_2 = self.get_number()
                    else:
                        raise Exception("Unexpected Poke Update Field ID", field_id)
        assert self.current_index == final_index

    @classmethod
    def encode_story_pokemons(cls, pokemons: list[Pokemon], url_parameters: dict) -> str:
        "Encode the pokemons for `loadStoryProfile`"
        pokemons.sort(key=lambda poke: poke.poke_party_pos)
        self = cls('', pokemons)
        self.write_number(len(pokemons))
        for i, pokemon in enumerate(pokemons, 1):
            url_parameters[f"PN{i}"] = pokemon.poke_nickname
            self.write_number(pokemon.pokedex_num)

            self.write_double_number(pokemon.poke_exp)
            self.write_number(pokemon.poke_lvl)

            self.write_number(pokemon.move_1_id)
            self.write_number(pokemon.move_2_id)
            self.write_number(pokemon.move_3_id)
            self.write_number(pokemon.move_4_id)

            self.write_number(pokemon.targetting_type)
            self.write_number(pokemon.poke_gender)
            
            self.write_double_number(pokemon.poke_save_id)
            
            self.write_number(pokemon.poke_party_pos)
            self.write_number(pokemon.poke_extra)
            self.write_number(pokemon.poke_held_item)

            self.write_string(pokemon.poke_is_hacked_tag)
            
            self.write_number(0)  # unused 1
            
            self.write_number(pokemon.poke_selected_move)
            self.write_number(pokemon.poke_selected_ability)

            self.write_number(0)  # unused 2
            self.write_number(0)  # unused 3

        return encode(self.get_length() + self.data)
