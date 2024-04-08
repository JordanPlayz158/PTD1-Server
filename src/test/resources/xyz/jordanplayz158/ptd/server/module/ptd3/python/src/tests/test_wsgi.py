import io
import random
import sys
from urllib.parse import quote

from src.main import app

path = '/ptd3save/username'

bodies = [
    (
        'Action=createAccount',
    ),
    (
        'Action=loadAccount',
    ),
    (
        'Action=saveStory',
        'whichProfile=2',
        # 'extra=Save=true&NewGameSave=true&Nickname=Etrotta&Color=2&Gender=0&CS=null&MSave=true&MA=ym&LevelSave=true&LevelA=y&LevelC=y&PokeNick1=Pichu&PokeNick2=Rattata',
        # It actually should be encoded here
        'extra=Save%3Dtrue%26NewGameSave%3Dtrue%26Nickname%3DEtrotta%26Color%3D2%26Gender%3D0%26CS%3Dnull%26MSave%3Dtrue%26MA%3Dym%26LevelSave%3Dtrue%26LevelA%3Dy%26LevelC%3Dy%26PokeNick1%3DPichu%26PokeNick2%3DRattata',
        'extra2=yeyyyyyy',
        'extra3=woeywyyyymyycyrwycyyrypwwwcqapycymyyyyymymymynyyymymymyyyymyywyoywyeyqyyycyqymyyyyyyymymynycymymym',
        'extra4=yeyyywyy',
        'extra5=pymw',
    ),
    (
        'Action=loadStory',
    ),
    (
        'Action=loadStoryProfile',
        'whichProfile=2',
    ),
]

bodies = [
    '&'.join(
        (
            *body,
            # 'Public' server. The Pass is just to interact with the server, not with deta itself, 
            # and I do not mind people having access to that game account.
            f'Email={quote(f"https://ptd3server-3-o2189185.deta.app{path}")}',
            'Pass=JgpTobfD5y3SCrjLEW1sGGhSFvX8gAod',
            )
        )
    .encode('UTF-8')
    for body in bodies
]

expected_responses = [
    b'Result=Success&UID=754&Reason=loadedAccount',
    b'Result=Success&UID=754&Reason=loadedAccount',
    b'Result=Success&CS=ywcqa&PID0=1&PID1=2',
    b'Result=Success&Nickname2=Etrotta&Version2=2&extra=wywywwymyyyy',
    # separated the last one since it is pretty damn long with loading the pokes
    b'\
        Result=Success\
        &extra=ypyyyy\
        &extra2=yeyyyyyy\
        &PN1=Pichu&PN2=Rattata\
        &extra3=woqywcyrwycyyrypwwwcqapycymyyyyyyyymymymynymyyymymymwyoywyeyqyyycyqymyyyyyywyyymymynymycymymym\
        &extra4=yeyyywyy\
        &CS=ywcqa\
        &extra5=aepw\
    '.replace(b' ',b''),
]

def get_environ(path: str, body: bytes) -> dict:
    environ = {
        "wsgi.input": io.BytesIO(body),
        "wsgi.errors": sys.stderr,
        'wsgi.version': (1, 0),
        'wsgi.url_scheme': 'http',
        'wsgi.multithread': False,
        'wsgi.multiprocess': False,
        'wsgi.run_once': True,
        
        "CONTENT_LENGTH": str(len(body)),
        "CONTENT_TYPE": "application/x-www-form-urlencoded",
        "PATH_INFO": path,
        "REMOTE_ADDR": "127.0.0.1",
        "REQUEST_METHOD": "POST",
        "SERVER_NAME": "localhost",
        "SERVER_PORT": "5000",
    }
    return environ


def test_wsgi():
    def start_response(status, headers):
        assert status == '200 OK'

    for body, expected_response in zip(bodies, expected_responses):
        random.seed(42)  # To make sure the UID will come back the same
        e = get_environ(path, body)
        response = iter(app(e, start_response))
        assert next(response) == expected_response
