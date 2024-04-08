from src.models.data_numbers import Number, DoubleNumber

translate_key = [('0', "m"), ('1', "y"), ('2', "w"), ('3', "c"), ('4', "q"), ('5', "a"), ('6', "p"), ('7', "r"), ('8', "e"), ('9', "o")]
encode_map = str.maketrans({numeric: letter for numeric, letter in translate_key})
decode_map = str.maketrans({letter: numeric for numeric, letter in translate_key})

def encode(string: str) -> str:
    "Encodes save strings (don't ask me why, that is just how sam does it in the client)"
    return string.translate(encode_map)
    
def decode(string: str) -> str:
    "Decodes save strings (don't ask me why, that is just how sam does it in the client)"
    return string.translate(decode_map)


class DataExtractor:
    def __init__(self, data: str) -> None:
        self.data = decode(data)
        self.current_index = 0
    
    def get_number(self) -> Number:
        number, self.current_index = Number.from_save(self.data, self.current_index)
        return number

    def get_double_number(self) -> DoubleNumber:
        number, self.current_index = DoubleNumber.from_save(self.data, self.current_index)
        return number
    
    def get_string(self) -> str:
        size = int(self.data[self.current_index])
        self.current_index += 1
        string = self.data[self.current_index : self.current_index + size]
        self.current_index += size
        return string
    
    def write_digit(self, number: int) -> None:
        'Writes a single digit'
        self.data += str(number)

    def write_number(self, number: int, prepend: bool = False) -> None:
        'Writes a single number in the format of (size, actual value)'
        if not isinstance(number, Number):
            number = Number(number)
        if prepend:
            self.data + number.to_save() + self.data
        else:
            self.data += number.to_save()
    
    def write_double_number(self, number: int, prepend: bool = False) -> None:
        'Writes a single number in the format of (number of size digits, size, actual value)'
        if not isinstance(number, DoubleNumber):
            number = DoubleNumber(number)
        if prepend:
            self.data + number.to_save() + self.data
        else:
            self.data += number.to_save()

    def write_string(self, string: str) -> None:
        'Writes a small string in the format of (length, actual string)'
        self.data += str(len(string)) + string

    def get_length(self) -> str:
        size = len(self.data)
        digits_len = len(str(size))
        final_len = digits_len + size + 1
        while len(str(final_len)) > digits_len:
            digits_len += 1
            final_len += 1
        signature_len = len(str(final_len))
        return str(signature_len) + str(final_len)
