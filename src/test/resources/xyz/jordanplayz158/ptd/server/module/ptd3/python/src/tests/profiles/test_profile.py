import pytest
from src.models.profile import ProfileLoader

def test_create(first_profile):
    extra = '''Save=true
    NewGameSave=true
    Nickname=Etrotta
    Color=2
    Gender=0
    CS=null
    MSave=true
    MA=ym
    LevelSave=true
    LevelA=y
    LevelC=y
    PokeNick1=Pichu
    PokeNick2=Rattata'''
    extra = dict(x.strip().split('=') for x in extra.splitlines())
    profile = ProfileLoader.update_profile(None, extra)
    assert profile == first_profile

def test_update(first_profile, second_profile):
    extra = '''Save=true
    CS=ywcqa
    LevelSave=true
    LevelA=w
    LevelC=w
    PokeNick3=Geodude'''
    extra = dict(x.strip().split('=') for x in extra.splitlines())
    profile = ProfileLoader.update_profile(first_profile, extra)
    assert profile == second_profile
