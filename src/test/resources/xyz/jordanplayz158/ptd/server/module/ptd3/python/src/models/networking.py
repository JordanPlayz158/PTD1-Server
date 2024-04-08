class Request:
    def __init__(self, body: str):
        data: dict[str, str] = {k: v for k, v in (x.split('=', 1) for x in body.split('&'))}
        self.data = data

    def __repr__(self) -> str:
        return f"Request({self.data=})"

class Response:
    def __init__(self, **kwargs):
        self.data = kwargs
        self.body = '&'.join(f'{k}={v}' for k, v in kwargs.items())

    def __repr__(self) -> str:
        return f"Response({self.data=}, {self.body=})"

    def encode(self) -> bytes:
        return self.body.encode("utf-8")
