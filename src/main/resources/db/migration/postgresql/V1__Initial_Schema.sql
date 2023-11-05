-- Potential advanced permission system
-- add privileges table (id and name)
-- add role_privileges table (id, role_id, privilege_id)
--   each row will be a privilege the role has
CREATE TABLE roles (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO roles (id, name)
VALUES (1, 'User'), (2, 'Admin');


CREATE TABLE users (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    -- SWF max length of 50
    email VARCHAR(50) NOT NULL UNIQUE,
    role_id BIGINT NOT NULL DEFAULT 1,
    -- SWF max length of 10
    -- But Bcrypt hashing has size of 72
    password VARCHAR(72) NOT NULL,
    dex VARCHAR(151) NOT NULL DEFAULT '',
    shiny_dex VARCHAR(151) NOT NULL DEFAULT '',
    shadow_dex VARCHAR(151) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_users_role_id_roles_id FOREIGN KEY (role_id)
        REFERENCES roles(id)
);


CREATE TABLE saves (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    number SMALLINT NOT NULL,
    -- advanced
    levels_completed SMALLINT NOT NULL DEFAULT 0,
    -- advanced_a
    levels_started SMALLINT NOT NULL DEFAULT 0,
    -- 8 String length check, cut to 8 characters if bigger
    nickname VARCHAR(8) NOT NULL DEFAULT 'Satoshi',
    -- no more than 8 badges possible
    badges SMALLINT NOT NULL DEFAULT 0,
    avatar VARCHAR(4) NOT NULL DEFAULT 'none',
    -- classic
    has_flash BOOLEAN NOT NULL DEFAULT FALSE,
    -- classic_a also called extra_info was removed as the
    -- data doesn't seem to need to be stored in the database
    challenge SMALLINT NOT NULL DEFAULT 0,
    money INTEGER NOT NULL DEFAULT 50,
    npc_trade BOOLEAN NOT NULL DEFAULT FALSE,
    -- shinyHunt was removed as client never reads it from
    -- server response (never asks for it) and it seems to
    -- do nothing from the swf codebase either (hardcoded to
    -- 0 in client response body to server)

    -- 1 for Red, 2 for Blue
    version SMALLINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_saves_user_id_num UNIQUE(user_id, number),
    CONSTRAINT FK_saves_user_id_users_id FOREIGN KEY (user_id)
            REFERENCES users(id) ON DELETE CASCADE
);


CREATE TABLE save_items (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    save_id BIGINT NOT NULL,
    item SMALLINT NOT NULL,
    count SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_save_items_save_id_item UNIQUE(save_id, item),
    CONSTRAINT FK_save_items_save_id_saves_id FOREIGN KEY (save_id)
                REFERENCES saves(id) ON DELETE CASCADE
);


CREATE TABLE pokemon (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    save_id BIGINT NOT NULL,
    -- SWF has pokemon id as an int rather
    -- than Number so only signed int32 range
    -- is allowed otherwise, id would be used
    -- TODO: For backend code, make auto_increment
    swf_id INTEGER NOT NULL,
    number SMALLINT NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    experience INTEGER NOT NULL DEFAULT 0,
    level SMALLINT NOT NULL DEFAULT 1,
    move_1 SMALLINT NOT NULL DEFAULT 1,
    move_2 SMALLINT NOT NULL DEFAULT 0,
    move_3 SMALLINT NOT NULL DEFAULT 0,
    move_4 SMALLINT NOT NULL DEFAULT 0,
    move_selected SMALLINT NOT NULL DEFAULT 1,
    ability SMALLINT NOT NULL DEFAULT 0,
    target_type SMALLINT NOT NULL DEFAULT 1,
    -- Default client ALWAYS populates pokemon with 'n'
    -- So, if no tag is submitted, default should be 'h'
    -- to signify hacked
    tag VARCHAR(3) NOT NULL DEFAULT 'h',
    -- item column removed as it is hardcoded to 0
    -- owner column removed as user id fills that role
    position INTEGER NOT NULL DEFAULT 1,
    rarity SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT UQ_pokemon_save_id_swf_id UNIQUE(save_id, swf_id),
    CONSTRAINT FK_pokemon_save_id_saves_id FOREIGN KEY (save_id)
            REFERENCES saves(id) ON DELETE CASCADE
);


CREATE TABLE achievements (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,

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

    CONSTRAINT FK_achievements_user_id_users_id FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);


CREATE TABLE achievement_redemptions (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    achievement_id BIGINT NOT NULL UNIQUE,
    completions SMALLINT NOT NULL DEFAULT 0,
    redemptions SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_achievement_redemptions_achievement_id_achievements_id FOREIGN KEY (achievement_id)
        REFERENCES achievements(id) ON DELETE CASCADE
);


CREATE TABLE settings (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    key VARCHAR(255) NOT NULL UNIQUE,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);


INSERT INTO settings(id, key, value)
VALUES(1, 'DB_MIGRATION_ASKED', 'FALSE');