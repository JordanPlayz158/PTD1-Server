import os
import itertools
from typing import Callable, Optional, Type, Union, overload
from datetime import datetime
import inspect

from deta import Base

from src.database.exceptions import KeyNotFound
from src.database.record import (
    Record,

)
from src.database.query import Query

from src.database._local_base import Base as LocalBase

# Instructions for encoding / decoding data not supported by deta base
EMPTY_LIST_STRING = "$EMPTY_LIST"  # Setting a field to an empty list sets it to `null`
EMPTY_DICTIONARY_STRING = "$EMPTY_DICT"  # Setting a field to an empty dictionaries seems to set it to `null`
DATETIME_STRING = "$ENCODED_DATETIME"  # Ease datetime conversion, not that I really need of it for PTD though
ESCAPE_STRING = "$NOOP"  # Do not mess up if the user input 'just happen' to start with a $COMMAND


class Database(dict[str, Record]):
    def __init__(self, name: str, record_type: Optional[Type[Record]] = None):
        """Deta Base wrapper | ORM
        
        Parameters
        ----------
        name : str
            Which name to use for the Deta Base
        record_type : RecordType
            Pass one to enforce a schema.
            Without passing one, you can use multiple different Record classes on the same base, though that is not recommended.
            You may either subclass the Record class or use it directly, though direct usage is not recommended
        """
        base_mode = os.getenv("DETA_ORM_DATABASE_MODE", "DETA_BASE")
        if base_mode == "DETA_BASE":
            self.__base = Base(name)
        elif base_mode == "MEMORY":
            self.__base = LocalBase(name, sync_disk=False)
        elif base_mode == "DISK":
            self.__base = LocalBase(name, sync_disk=True)
        else:
            raise Exception("Invalid value for DETA_ORM_DATABASE_MODE environment variable")

        self._record_type = record_type
        self.__known_functions = {}
    
    def _check_type(self, record) -> None:
        "Validates if a record matches the schema passed on class initialisation."
        if (
            isinstance(record, Record) 
            and self._record_type is not None 
            and not isinstance(record, self._record_type)
        ):
            raise TypeError(f"Record {record} does not matches database assigned Record type {self._record_type}")

    def remember_function(self, function: Callable):
        """Use as a decorator to be able to save/load a function reference in Records."""
        self.__known_functions[function.__name__] = function
        self.__known_functions[function] = function.__name__

    def __getitem__(self, key: str) -> Record:
        """Retrieves an item from the database.
        If the key if not found, raises a KeyError.
        """
        result = self.get(key)
        if result is None:
            raise KeyError(key)
        return result

    def __setitem__(self, key: str, record: Record) -> None:
        self.put(key, record)
    
    def __delitem__(self, key: str) -> None:
        self.delete(key)

    @overload
    def encode_entry(self, record: Union[list, tuple]) -> list: ...
    @overload
    def encode_entry(self, record: Union[dict, Record]) -> dict: ...
    def encode_entry(self, record: Union[dict, Record, list, tuple]) -> Union[dict, list]:
        "Converts values so that we can store it properly in Deta Base. Does not modifies in-place."
        # Convert and shallow copy
        if isinstance(record, Record):
            record = record.to_dict()
        elif isinstance(record, tuple):
            record = list(record)
        else:
            record = record.copy()
        # Thing we need to iterate through and overwrite on the record
        if isinstance(record, list):
            items = enumerate(list(record))
        else:
            items = record.items()
        for key, value in items:
            if isinstance(value, dict) and dict(value) == {}:  # Empty dict becomes `null` on deta base on update
                record[key] = EMPTY_DICTIONARY_STRING
            elif isinstance(value, (list, tuple)) and list(value) == []:  # Empty lists becomes `null` on deta base on update
                record[key] = EMPTY_LIST_STRING
            elif inspect.isfunction(value):  # TODO register functions via a decorator and save/load them
                if value in self.__known_functions:
                    record[key] = {
                        "__database_load_method": "function",
                        "__name": self.__known_functions[value],
                    }
                else:
                    raise ValueError(f"""Unexpected function: {value}. \
                    If you want to save it in the database, please use the `@base.remember_function` decorator.""")
                # This should only be used if this record is only going to be stored for a short amount of time
                # And even then, it should be using sparingly
            elif isinstance(value, (dict, Record)):  # Convert nested fields 
                record[key] = self.encode_entry(value)
            elif isinstance(value, (list, tuple)):  # Convert all list elements
                record[key] = [
                    self.encode_entry(element)
                    if isinstance(element, (dict, Record, list, tuple))
                    else element 
                    for element in value
                ]
            elif isinstance(value, datetime):  # Ease datetime conversion
                record[key] = DATETIME_STRING + value.isoformat()
            elif isinstance(value, str) and value.startswith("$"):  # essentially escape '$'
                record[key] = ESCAPE_STRING + value
        return record

    def _load_encoded(self, record: dict) -> Union[Record, Callable]:
        "Tries to load back an encoded record field into a user defined class or function. May raise exceptions."
        method = record['__database_load_method']
        if method == "Record.from_dict":
            cls = Record._known_database_models[record["__class_name"]]
            return cls.from_dict(record)
        # <compat> Temporary lib upgrade compatibility patch
        # will not be present on the deta orm library release, but leaving here 
        # in the ultra rare case of anyone being running an older version of ptd3server
        elif method == "LoadableDataclass.from_dict":
            cls = Record._known_database_models[record["__class_name"]]
            return cls.from_dict(record)
        # </compat>
        elif method == "function":
            name = record['__name']
            return self.__known_functions[name]

    @overload
    def decode_entry(self, record: list) -> list: ...
    @overload
    def decode_entry(self, record: dict) -> Record: ...
    def decode_entry(self, record: Union[dict, list]) -> Union[dict, Record, list]:
        "Converts back some changes that we may make when storing. May modify in-place."
        if isinstance(record, list):
            items = enumerate(record)
        else:
            items = record.items()

        for key, value in items:
            if isinstance(value, dict):  # Make sure we hit nested fields
                record[key] = self.decode_entry(value)
            elif isinstance(value, list):  # Convert all list elements.
                record[key] = [
                    self.decode_entry(element) 
                    if isinstance(element, (dict, list)) 
                    else element
                    for element in value
                ]
            elif isinstance(value, str):  # Revert our custom 'special' strings
                if value == EMPTY_DICTIONARY_STRING:  # Empty dict may become `null` on deta base
                    record[key] = {}
                elif value == EMPTY_LIST_STRING:  # Empty lists may become `null` on deta base
                    record[key] = []
                elif value.startswith(DATETIME_STRING):  # Ease datetime conversion
                    record[key] = datetime.fromisoformat(value.removeprefix(DATETIME_STRING))
                elif value.startswith(ESCAPE_STRING):  # Escape strings starting with `$`
                    record[key] = value.removeprefix(ESCAPE_STRING)

        if '__database_load_method' in record:
            return self._load_encoded(record)
        return record

    def get(self, key: str) -> Optional[Record]:
        """Retrieve a record based on it's key. 
        If it does not exists, returns None"""
        data = self.__base.get(str(key))
        if data is None:
            return None
        result = self.decode_entry(data)
        self._check_type(result)
        return result

    def insert(self, key: str, data: Union[Record, dict], *, expire_in: Optional[int] = None, expire_at: Optional[datetime] = None) -> None:
        "Insert a record. Errors if it already exists."
        if isinstance(data, Record):
            self._check_type(data)
            data = data.to_dict()
        self.__base.insert(self.encode_entry(data), str(key), expire_in=expire_in, expire_at=expire_at)
    
    def put(self, key: str, data: Union[Record, dict], *, expire_in: Optional[int] = None, expire_at: Optional[datetime] = None) -> None:
        "Insert a record, or overwrite it if it already exists."
        if isinstance(data, Record):
            self._check_type(data)
            data = data.to_dict()
        self.__base.put(self.encode_entry(data), str(key), expire_in=expire_in, expire_at=expire_at)
    
    def delete(self, key: str) -> None:
        "Deletes a record."
        self.__base.delete(str(key))

    def _put_many_list(self, data: list[list[Union[dict, Record]]], key_source: Callable[[Union[dict, Record]], str]) -> None:
        for sub_list in data:
            records = []
            for record in sub_list:
                self._check_type(record)
                _key = key_source(record)
                record = self.encode_entry(record)
                record['key'] = _key
                records.append(record)
            if records:
                self.__base.put_many(records)

    def _put_many_dict(self, data: list[tuple[str, Union[dict, Record]]]) -> None:
        for sub_dict_items in data:
            records = []
            for k, record in sub_dict_items:
                self._check_type(record)
                record = self.encode_entry(record)
                record['key'] = k
                records.append(record)
            if records:
                self.__base.put_many(records)

    @overload  # Putting a dictionary of str -> Record
    def put_many(
        self,
        data: dict[str, Union[Record, dict]],
        *,
        key_source: None = None,
        iter: bool = False,
    ) -> None: ...
    @overload  # Putting a List of Records
    def put_many(
        self,
        data: list[Union[Record, dict]],
        *,
        key_source: Union[str, Callable[[Union[Record, dict]], str]] = None,
        iter: bool = False
    ) -> None: ...
    def put_many(  # Actual definition
        self,
        data: Union[list[Union[Record, dict]], dict[str, Union[Record, dict]]],
        *,
        key_source: Union[str, Callable[[Union[Record, dict]], str], None] = None,
        iter: bool = False,
    ) -> None:
        """Insert or overwrite multiple records and return them. 
        Deta Base has a limit of up to 25 records at once without `iter`

        Parameters
        ----------
        data : list of records or dictionary
            If a list of records: 
                If key_source is a string: Use that record attribute or dict value as the key
                If the key_source is a function: Calls it for each Record or dict
            If a dictionary: Uses the dictionary's keys for their respective records
        key_source: str, function or None
            Which field to use as the `key` for each record in data.
            If it's callable, it will be called for each record
            Ignored when using a dictionary
        iter : bool, default False
            Automatically split the data into sublists of up to 25 items and put multiple times.
            If set to True, this function may use multiple HTTPS requests.
        """
        if isinstance(data, list):
            if iter:
                data_chain = itertools.chain(data[offset:offset+25] for offset in range(0, len(data), 25))
            else:
                data_chain = [data]
            if isinstance(key_source, str):
                key_field = key_source
                key_source = lambda record: str(getattr(record, key_field, None) or record[key_field])
            self._put_many_list(data_chain, key_source)
        elif isinstance(data, dict):
            if iter:
                _it = iter(data.items())
                data_chain = (itertools.islice(_it, 25) for _ in range(((len(data)-1) // 25) + 1))
            else:
                data_chain = [data.items()]
            self._put_many_dict(data_chain)
        else:
            raise TypeError(f"Unsupported type {type(data)} passed to {self}.put_many: {data!r}")

    def fetch(
        self,
        query: Union[Query, dict, list[dict], None] = None,
        limit: int = 1000,
        last: Optional[str] = None,
        follow_last: bool = False,
    ) -> list[Record]:
        """Returns multiple items from the database based on a query.

        Parameters
        ----------
        query : Query
            See the `Query` and `Field` classes as well as https://docs.deta.sh/docs/base/queries/ for more information.
        limit : int, default 1000
            Maximum number of records to fetch.
            NOTE: Deta Base will only retrieve up to 1MB of data at a time, and that is before applying the filters
        last : str, default None
            Equivalent to `offset` in normal databases, but key based instead of position based
        follow_last : bool, default False
            Automatically fetch more records up to `limit` if the query returns a `last` element
        """
        if isinstance(query, Query):
            query = query.to_list()
        result = self.__base.fetch(query, limit=limit, last=last)
        records = result.items
        if follow_last:
            while result.last is not None and len(records) < limit:
                result = self.__base.fetch(query, limit=limit, last=result.last)
                records.extend(result.items)

        result = []
        for record in records:
            loaded = self.decode_entry(record)
            self._check_type(loaded)
            result.append(loaded)
        return result

    def update(self, key: str, updates: dict) -> None:
        """Updates a Record in the database. Local representations of it might become outdated."""
        updates = self.encode_entry(updates)
        try:
            self.__base.update(updates, key)
        except Exception as err:
            import re
            reason = err.args[0] if err.args else ''
            if isinstance(reason, str) and re.fullmatch(r"Key \'.*\' not found", reason):
                raise KeyNotFound(reason)
            else:
                raise
