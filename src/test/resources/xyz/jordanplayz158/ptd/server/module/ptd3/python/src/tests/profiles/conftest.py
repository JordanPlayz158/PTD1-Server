import pytest
from src.models.profile import Profile

@pytest.fixture
def first_profile():
    "The 'test' player's newly created profile after the first save"
    return Profile(
        profile_id=1,
        profile_nickname='Etrotta',
        game_version='2',
        gender='0',
        money=10,
        max_level_accomplished=1,
        max_level_complete=1,
    )

@pytest.fixture
def second_profile():
    "The 'test' player's profile after the second save"
    return Profile(
        profile_id=1,
        profile_nickname='Etrotta',
        game_version='2',
        gender='0',
        money=10,
        max_level_accomplished=2,
        max_level_complete=2,
    )
