from dataclasses import dataclass

from src.models.extractor import DataExtractor, encode
from src.database import Record

@dataclass
class ExtraInfo(Record):
    info_id: int
    info_value: int

class ExtraInfoLoader(DataExtractor):
    "Loads extrainformation from `saveStory` -> `extra2`"
    def __init__(self, data: str, infos: list[ExtraInfo] = []) -> None:
        super().__init__(data)
        self.infos = infos

    def load(self):
        final_index = self.get_number().value
        number_of_infos = self.get_number().value
        for info_index in range(number_of_infos):
            info = ExtraInfo(
                info_id = self.get_number().value,
                info_value = self.get_number().value,
            )
            self.infos.append(info)
        assert self.current_index == final_index

    @classmethod
    def encode_story_extras(cls, infos: list[ExtraInfo], url_parameters: dict) -> str:
        "Encode the extra variables for `loadStoryProfile`"
        self = cls('', infos)
        self.write_number(len(infos))
        for info in infos:
            self.write_number(info.info_id)
            self.write_number(info.info_value)
        return encode(self.get_length() + self.data)
