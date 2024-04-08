CREATE TABLE ptd2_users (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    gen_1_dex VARCHAR(151) NOT NULL DEFAULT '',
    gen_2_dex VARCHAR(100) NOT NULL DEFAULT '',
    gen_3_dex VARCHAR(135) NOT NULL DEFAULT '',
    gen_4_dex VARCHAR(107) NOT NULL DEFAULT '',
    gen_5_dex VARCHAR(156) NOT NULL DEFAULT '',
    gen_6_dex VARCHAR(90) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_ptd2_users_user_id_users_id FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE ptd2_saves (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    number TINYINT(1) NOT NULL,
    current_map TINYINT(3) NOT NULL DEFAULT 3,
    map_spot TINYINT(3) NOT NULL DEFAULT 1,
    nickname VARCHAR(40) NOT NULL,
    gender TINYINT(1) NOT NULL,
    money INT(5) NOT NULL DEFAULT 10,
    --current_save VARCHAR(15) NOT NULL DEFAULT 0,
    current_time TINYINT(4) NOT NULL,
    version TINYINT(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd2_saves_user_id_number UNIQUE(user_id, number),
    CONSTRAINT FK_ptd2_saves_user_id_ptd2_users_id FOREIGN KEY(user_id)
        REFERENCES ptd2_users (id) ON DELETE CASCADE
);

CREATE TABLE ptd2_pokemon (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    save_id INTEGER NOT NULL,
    swf_id INT(10) NOT NULL,
    number SMALLINT(3) NOT NULL,
    nickname VARCHAR(25) NOT NULL,
    experience INT(7) NOT NULL DEFAULT 0,
    level TINYINT(3) NOT NULL DEFAULT 1,
    move_1 SMALLINT(3) NOT NULL,
    move_2 SMALLINT(3) NOT NULL,
    move_3 SMALLINT(3) NOT NULL,
    move_4 SMALLINT(3) NOT NULL,
    target_type TINYINT(1) NOT NULL,
    tag VARCHAR(2) NOT NULL DEFAULT 'h',
    position INT(5) NOT NULL,
    gender TINYINT(1) NOT NULL,
    extra TINYINT(1) NOT NULL,
    item TINYINT(2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd2_pokemon_save_id_swf_id UNIQUE(save_id, swf_id),
    CONSTRAINT FK_ptd2_pokemon_save_id_ptd2_saves_id FOREIGN KEY (save_id)
        REFERENCES ptd2_saves (id) ON DELETE CASCADE
);

CREATE TABLE ptd2_extras (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    save_id INTEGER NOT NULL,
    is_item BOOLEAN NOT NULL,
    number SMALLINT(3) NOT NULL,
    value INT(2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_ptd2_extras_save_id_ptd2_saves_id FOREIGN KEY (save_id)
        REFERENCES ptd2_saves (id) ON DELETE CASCADE
);

CREATE TABLE `ptd2_1v1s` (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    number TINYINT(2) NOT NULL,
    money INT(5) NOT NULL,
    levels_unlocked TINYINT(2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd2_1v1s_user_id_number UNIQUE(user_id, number),
    CONSTRAINT FK_ptd2_1v1s_user_id_ptd2_users_id FOREIGN KEY(user_id)
        REFERENCES ptd2_users (id) ON DELETE CASCADE
);