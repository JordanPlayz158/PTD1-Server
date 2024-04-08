from dataclasses import dataclass

from src.models.extractor import DataExtractor, encode
from src.database import Record

@dataclass
class Item(Record):
    item_id: int
    item_quantity: int

class ItemsLoader(DataExtractor):
    "Loads inventory (items) from `saveStory` -> `extra4`"
    def __init__(self, data: str, items: list[Item]) -> None:
        super().__init__(data)
        self.items = items

    def load(self):
        final_index = self.get_number().value
        number_of_items = self.get_number().value
        for info_index in range(number_of_items):
            item = Item(
                item_id = self.get_number().value,
                item_quantity = self.get_number().value,
            )
            self.items.append(item)
        assert self.current_index == final_index

    @classmethod
    def encode_story_items(cls, items: list[Item], url_parameters: dict) -> str:
        "Encode the items for `loadStoryProfile`"
        self = cls('', items)
        self.write_number(len(items))
        for item in items:
            self.write_number(item.item_id)
            self.write_number(item.item_quantity)
        return encode(self.get_length() + self.data)
