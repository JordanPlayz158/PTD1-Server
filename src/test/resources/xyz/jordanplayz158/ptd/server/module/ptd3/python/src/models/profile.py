from dataclasses import dataclass
from typing import Optional

from src.database.record import Record
from src.models.extractor import DataExtractor, decode, encode

@dataclass
class Profile(Record):
    # email: str
    # password: str
    profile_id: int  # 1, 2, 3
    
    profile_nickname: str
    game_version: str  # Omega / Alpha (like red|blue)
    gender: str
    money: str
    max_level_accomplished: str
    max_level_complete: str

    @classmethod
    def default(cls):
        return cls(1, 'Red', 2, 0, 10, 1, 1)


class ProfileLoader(DataExtractor):
    "Loads Story Profiles"
    def __init__(self, data: str, profiles: list[Profile]) -> None:
        super().__init__(data)
        self.profiles = profiles

    @classmethod
    def update_profile(cls, profile: Optional[Profile], data: dict[str, str]) -> Profile:
        if profile is None:
            # Don't technically need to check value of NewGameSave
            #  as the variable is not sent period unless it is true
            assert data.get("NewGameSave") == "true"
            profile = Profile.default()

        # Save is always sent hardcoded to true
        # Is true for most of these
        assert data.get("Save") == "true"

        if data.get("NewGameSave") == "true":
            profile.profile_nickname = data['Nickname']
            profile.game_version = (data['Color'])
            profile.gender = (data['Gender'])
        if data.get("MSave") == "true":
            # not a data extractor 'Number' (with n_digits) - but still 'encoded'
            profile.money = int(decode(data['MA']))
        if data.get("LevelSave") == "true":
            profile.max_level_accomplished = int(decode(data['LevelA']))
            profile.max_level_complete = int(decode(data['LevelC']))
        
        return profile

    @classmethod
    def encode_profiles(cls, profiles: list[Profile], url_parameters: dict) -> str:
        "Encode all of the user's profiles for `loadStory`"
        self = cls('', profiles)
        self.write_digit(len(profiles))
        for profile in profiles:
            self.write_digit(profile.profile_id)
            self.write_string(str(profile.money))
            self.write_string(str(profile.max_level_complete))
            self.write_string(str(profile.max_level_accomplished))
            url_parameters[f'Nickname{profile.profile_id}'] = profile.profile_nickname
            url_parameters[f'Version{profile.profile_id}'] = profile.game_version

        return encode(self.get_length() + self.data)

    @classmethod
    def encode_story_profile(cls, profile: Profile, url_parameters: dict) -> str:
        "Encode one profile for `loadStoryProfile`"
        self = cls('', [profile])
        self.write_number(int(profile.max_level_complete))
        self.write_number(int(profile.max_level_accomplished))
        return encode(self.get_length() + self.data)
