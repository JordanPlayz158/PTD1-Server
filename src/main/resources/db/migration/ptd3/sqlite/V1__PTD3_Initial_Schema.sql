CREATE TABLE ptd3_saves (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    number TINYINT(1) NOT NULL,
    levels_completed TINYINT(3) NOT NULL DEFAULT 1,
    levels_accomplished TINYINT(3) NOT NULL DEFAULT 1,
    nickname VARCHAR(40) NOT NULL,
    money INT(5) NOT NULL DEFAULT 10,
    gender TINYINT(1) NOT NULL,
    version TINYINT(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd3_saves_user_id_number UNIQUE(user_id, number),
    CONSTRAINT FK_ptd3_saves_user_id_users_id FOREIGN KEY(user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE ptd3_pokemons (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    save_id INTEGER NOT NULL,
    swf_id INTEGER NOT NULL,
    number SMALLINT(3) NOT NULL,
    nickname VARCHAR(40) NOT NULL,
    experience INT(7) NOT NULL DEFAULT 0,
    level TINYINT(3) NOT NULL DEFAULT 1,
    move_1 SMALLINT(3) NOT NULL,
    move_2 SMALLINT(3) NOT NULL,
    move_3 SMALLINT(3) NOT NULL,
    move_4 SMALLINT(3) NOT NULL,
    move_selected TINYINT(2) NOT NULL DEFAULT 1,
    target_type TINYINT(1) NOT NULL,
    tag VARCHAR(2) NOT NULL DEFAULT 'h',
    position INT(5) NOT NULL,
    gender TINYINT(1) NOT NULL,
    extra TINYINT(1) NOT NULL,
    item TINYINT(2) NOT NULL,
    ability TINYINT(2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd3_pokemons_save_id_swf_id UNIQUE(save_id, swf_id),
    CONSTRAINT FK_ptd3_pokemons_save_id_ptd3_saves_id FOREIGN KEY (save_id)
        REFERENCES ptd3_saves (id) ON DELETE CASCADE
);

CREATE TABLE ptd3_extras (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    save_id INTEGER NOT NULL,
    is_item BOOLEAN NOT NULL,
    number SMALLINT(3) NOT NULL,
    value INT(2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_ptd3_extras_save_id_ptd3_saves_id FOREIGN KEY (save_id)
        REFERENCES ptd3_saves (id) ON DELETE CASCADE
);