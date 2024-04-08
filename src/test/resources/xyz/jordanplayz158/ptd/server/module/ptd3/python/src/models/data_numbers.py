class Number:
    def __init__(self, value: int):
        self.value = value
    
    def __repr__(self) -> str:
        return f"Number({self.value})"

    def to_save(self) -> str:
        assert 0 <= self.value < (10 ** 10)
        string = str(self.value)
        return str(len(string)) + string

    @classmethod
    def from_save(cls, string: str, current_index: int) -> tuple['Number', int]:
        """
        Parameters:
            string : Save string
            current_index : Current position of the 'read cursor'
        Returns:
            number : Number
            new_index : Position to set to after loading this number
        """
        n_pos = int(string[current_index])
        current_index += 1
        val = string[current_index:current_index + n_pos]
        current_index += n_pos
        return cls(int(val)), current_index


class DoubleNumber(Number):
    def __repr__(self) -> str:
        return f"DoubleNumber({self.value})"

    def to_save(self) -> str:
        assert 0 <= self.value < (10 ** 100)
        # Length of the actual value
        string = str(self.value)
        return Number(len(string)).to_save() + string

    @classmethod
    def from_save(cls, string: str, current_index: int) -> tuple['DoubleNumber', int]:
        """
        Parameters:
            string : str
                Save string
            current_index : int
                Current position of the 'read cursor'
        Returns:
            number : DoubleNumber
                Number loaded from the string
            new_index : int
                Position to set to after loading this number
        """
        size, current_index = Number.from_save(string, current_index)

        val = string[current_index:current_index + size.value]

        return cls(int(val)), current_index + size.value
