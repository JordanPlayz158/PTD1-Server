from dataclasses import dataclass
from src.database import Database, Record

@dataclass
class Foo(Record):
    name: str
    age: int
    id: str
    _ignore: int = 1

def test_typed():
    db_typed = Database("test_db", Foo)
    foo = Foo('bob', 20, '123', 2)
    assert foo.name == 'bob', foo._ignore == 2
    db_typed['record'] = foo
    copy = db_typed['record']
    assert copy.name == 'bob', copy._ignore == 1
    del db_typed['record']
    assert db_typed.get('record') is None

def test_typeless():
    db_typeless = Database("test_db")
    foo = Foo('bob', 20, '123', 2)
    assert foo.name == 'bob', foo._ignore == 2
    db_typeless['record'] = foo
    copy = db_typeless['record']
    assert copy.name == 'bob', copy._ignore == 1
    del db_typeless['record']
    assert db_typeless.get('record') is None
