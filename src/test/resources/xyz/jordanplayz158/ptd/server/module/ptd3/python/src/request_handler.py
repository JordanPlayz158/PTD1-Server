import random

from werkzeug.datastructures import ImmutableMultiDict

from src.models.networking import Request, Response
from src.models.extractor import encode
from src.manage_save import (
    get_profiles_list,
    get_story_profile,
    set_save_data,
    delete_save_data,
)

CS = encode("12345")


def get_uid():
    # Used in-game to determine one thing (`get_Trainer_Specific_Elemental` function)
    return str(random.randint(100, 1000))


def create_check_sum(data: str) -> int:
    "Creates a checksum"
    return sum(ord(x) - 96 for x in data.lower()) * 3 + 45


def handle_request(data: ImmutableMultiDict[str, str]):
    action = data.get('Action')
    # match/case would look better, but for now Python 3.9 support is a requirement
    if action == 'createAccount':  # From: screen.screen_Main @ Register
        return Response(Result='Success', UID=get_uid(), Reason='loadedAccount')
    elif action == 'loadAccount':  # From: screen.screen_Main @ Login
        return Response(Result='Success', UID=get_uid(), Reason='loadedAccount')
    elif action == 'loadStory':  # From: screen.popup_StoryLoad
        return Response(Result='Success', **get_profiles_list(data))
    elif action == 'loadStoryProfile':  # From: screen.popup_Story_Profile_Load
        profile_data = get_story_profile(data)
        _check_sum = encode(str(create_check_sum(profile_data['extra3'] + CS)))
        return Response(Result='Success', **profile_data, CS=CS, extra5=_check_sum)
    elif action == 'saveStory':  # From: scren.popup_Story_Save
        return Response(Result='Success', CS=CS, **set_save_data(data))
    elif action == 'deleteStory':  # From: scren.popup_Story_Delete
        delete_save_data(data)
        return Response(Result='Success')
    else:
        raise Exception(f"Unexpected action: {data.get('Action')}", data)
