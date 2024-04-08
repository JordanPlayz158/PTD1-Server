import dataclasses
import copy
import typing

class Record:
    """Base class used to interface with the Database.

    Direct usage is not recommended. Either:
    - Subclass and use with dataclasses @dataclass.
    - Subclass and overwrite `__init__`, (@classmethod) `from_dict`, `to_dict`
    """

    _known_database_models = {}
    _database_model_name = None

    def __init_subclass__(cls, database_model_name: typing.Optional[str] = None) -> None:
        "Register known Record classes as models for encoding/decoding database records"
        # Intentionally not using `__qualname__` to make it not care about where it is defined
        name = database_model_name or cls.__name__
        if (existing_model := Record._known_database_models.get(name)) is not None:
            raise Exception(f"Cannot create class {cls} with same database model name as {existing_model=}")
        Record._known_database_models[name] = cls
        cls._database_model_name = name

    def __init__(self, **kwargs):
        "Direct usage of the Record class is not advisable."
        for k, v in kwargs.items():
            setattr(self, k, v)

    @classmethod
    def from_dict(cls, data: dict, *, strict: bool = True) -> 'Record':
        """
        Construct the Record from a dictionary, ignoring keys starting with `_`

        Use `strict=False` if you want to fill in missing fields with `None` instead of using the default value or raising an error
        """
        if dataclasses.is_dataclass(cls):
            if strict:  # Strict: If it is None, use the class's default value (or error if there isn't one)
                data = {
                    field.name: value for field in dataclasses.fields(cls)
                    if (value := data.get(field.name)) is not None
                }
            else:  # Non-strict: If it's None, pass it as None
                data = {
                    field.name: data.get(field.name) for field in dataclasses.fields(cls)
                }
        else:
            data = {k: v for k, v in data if not k.startswith("_")}
        try:
            return cls(**data)
        except TypeError:
            raise TypeError(f"Error while loading class {cls!r}, parameters supplied: {data}")

    def to_dict(self) -> dict:
        "Converts into a dictionary fit for storing in the Deta Base"
        data = copy.copy(self)
        for attr, val in vars(data).items():
            if isinstance(val, Record):
                setattr(data, attr, val.to_dict())
        if dataclasses.is_dataclass(data):
            data = dataclasses.asdict(data)
        else:
            data = {k: v for k, v in vars(data).items() if not k.startswith("_")}

        data["__database_load_method"] = "Record.from_dict"
        data["__class_name"] = self._database_model_name

        return data
