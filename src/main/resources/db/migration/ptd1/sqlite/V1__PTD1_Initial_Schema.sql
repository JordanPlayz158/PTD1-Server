CREATE TABLE ptd1_users (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    dex VARCHAR(151) NOT NULL DEFAULT '',
    shiny_dex VARCHAR(151) NOT NULL DEFAULT '',
    shadow_dex VARCHAR(151) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_ptd1_users_user_id_users_id FOREIGN KEY (user_id)
        REFERENCES users(id)
);


CREATE TABLE ptd1_saves (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    number TINYINT(1) NOT NULL,
    -- advanced
    levels_completed TINYINT(2) NOT NULL DEFAULT 0,
    -- advanced_a
    levels_started TINYINT(2) NOT NULL DEFAULT 0,
    -- 8 String length check, cut to 8 characters if bigger
    nickname VARCHAR(8) NOT NULL DEFAULT 'Satoshi',
    -- no more than 8 badges possible
    badges TINYINT(1) NOT NULL DEFAULT 0,
    avatar VARCHAR(4) NOT NULL DEFAULT 'none',
    -- classic
    has_flash BOOLEAN NOT NULL DEFAULT FALSE,
    -- classic_a also called extra_info was removed as the
    -- data doesn't seem to need to be stored in the database
    challenge TINYINT(1) NOT NULL DEFAULT 0,
    money INT(10) NOT NULL DEFAULT 50,
    npc_trade BOOLEAN NOT NULL DEFAULT FALSE,
    -- shinyHunt was removed as client never reads it from
    -- server response (never asks for it) and it seems to
    -- do nothing from the swf codebase either (hardcoded to
    -- 0 in client response body to server)

    -- 1 for Red, 2 for Blue
    version TINYINT(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd1_saves_user_id_number UNIQUE(user_id, number),
    CONSTRAINT FK_ptd1_saves_user_id_ptd1_users_id FOREIGN KEY (user_id)
            REFERENCES ptd1_users(id) ON DELETE CASCADE
);


CREATE TABLE ptd1_save_items (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    save_id INTEGER NOT NULL,
    item SMALLINT(3) NOT NULL,
    quantity TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd1_save_items_save_id_item UNIQUE(save_id, item),
    CONSTRAINT FK_ptd1_save_items_save_id_ptd1_saves_id FOREIGN KEY (save_id)
                REFERENCES ptd1_saves(id) ON DELETE CASCADE
);


CREATE TABLE ptd1_pokemon (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    save_id INTEGER NOT NULL,
    -- SWF has pokemon id as an int rather
    -- than Number so only signed int32 range
    -- is allowed otherwise, id would be used
    -- TODO: For backend code, make auto_increment
    swf_id INT(10) NOT NULL,
    number SMALLINT(3) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    experience INT(10) NOT NULL DEFAULT 0,
    level SMALLINT(3) NOT NULL DEFAULT 1,
    move_1 SMALLINT(3) NOT NULL DEFAULT 1,
    move_2 SMALLINT(3) NOT NULL DEFAULT 0,
    move_3 SMALLINT(3) NOT NULL DEFAULT 0,
    move_4 SMALLINT(3) NOT NULL DEFAULT 0,
    move_selected TINYINT(1) NOT NULL DEFAULT 1,
    ability TINYINT(1) NOT NULL DEFAULT 0,
    target_type TINYINT(1) NOT NULL DEFAULT 1,
    -- Default client ALWAYS populates pokemon with 'n'
    -- So, if no tag is submitted, default should be 'h'
    -- to signify hacked
    tag VARCHAR(3) NOT NULL DEFAULT 'h',
    -- item column removed as it is hardcoded to 0
    -- owner column removed as user id fills that role
    position INT(10) NOT NULL DEFAULT 1,
    rarity TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_ptd1_pokemon_save_id_swf_id UNIQUE(save_id, swf_id),
    CONSTRAINT FK_ptd1_pokemon_save_id_ptd1_saves_id FOREIGN KEY (save_id)
            REFERENCES ptd1_saves(id) ON DELETE CASCADE
);


CREATE TABLE ptd1_achievements (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,

    -- Per account achievements
    -- 1 prize 1 completion

    -- Shiny Hunter 1
    shiny_hunter_rattata BOOLEAN NOT NULL DEFAULT FALSE,
    shiny_hunter_pidgey BOOLEAN NOT NULL DEFAULT FALSE,
    shiny_hunter_geodude BOOLEAN NOT NULL DEFAULT FALSE,
    shiny_hunter_zubat BOOLEAN NOT NULL DEFAULT FALSE,

    star_wars BOOLEAN NOT NULL DEFAULT FALSE,
    no_advantage BOOLEAN NOT NULL DEFAULT FALSE,
    win_without_wind BOOLEAN NOT NULL DEFAULT FALSE,
    needs_more_candy BOOLEAN NOT NULL DEFAULT FALSE,


    -- Per save achievements
    -- 1 prize per completion (3 prizes max)
    the_hard_way BOOLEAN NOT NULL DEFAULT FALSE,


    -- 3 prizes 1 completion
    pewter_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    cerulean_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    vermillion_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    celadon_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    saffron_city_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    fuchsia_gym_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    cinnabar_gym_challenge BOOLEAN NOT NULL DEFAULT FALSE,
    viridian_city_challenge BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_ptd1_achievements_user_id_ptd1_users_id FOREIGN KEY (user_id)
        REFERENCES ptd1_users(id) ON DELETE CASCADE
);


CREATE TABLE ptd1_achievement_redemptions (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    achievement_id INTEGER NOT NULL,
    completions TINYINT(1) NOT NULL DEFAULT 0,
    redemptions TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_ptd1_achievement_redemptions_achievement_id_ptd1_achievements_id FOREIGN KEY (achievement_id)
        REFERENCES ptd1_achievements(id) ON DELETE CASCADE
);