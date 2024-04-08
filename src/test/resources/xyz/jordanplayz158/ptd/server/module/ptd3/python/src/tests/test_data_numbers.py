from src.models.data_numbers import Number, DoubleNumber

def test_data_numbers():
    string = "1521121512345123451234511"
    # (1 5) (2 11) (2 15 12345_12345_12345) (1 1)

    current_index = 0
    n_1, current_index = Number.from_save(string, current_index)
    n_2, current_index = Number.from_save(string, current_index)
    n_3, current_index = DoubleNumber.from_save(string, current_index)
    n_4, current_index = Number.from_save(string, current_index)

    assert n_1.value == 5
    assert n_2.value == 11
    assert n_3.value == 123451234512345
    assert n_4.value == 1

    assert current_index == len(string)

    assert (n_1.to_save() + n_2.to_save() + n_3.to_save() + n_4.to_save()) == string
