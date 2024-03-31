CREATE TABLE accounts (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(50) NOT NULL UNIQUE,
    pass VARCHAR(255) NOT NULL,
    dex1 VARCHAR(151),
    dex2 VARCHAR(100),
    dex3 VARCHAR(135),
    dex4 VARCHAR(107),
    dex5 VARCHAR(156),
    dex6 VARCHAR(90)
);

CREATE TABLE stories (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    num TINYINT(1) NOT NULL,
    Nickname VARCHAR(40) NOT NULL,
    Color TINYINT(1) NOT NULL,
    Gender TINYINT(1) NOT NULL,
    Money INT(5) NOT NULL,
    MapLoc TINYINT(3) DEFAULT 3,
    MapSpot TINYINT(3) DEFAULT 1,
    CurrentSave VARCHAR(15),
    CurrentTime TINYINT(4) NOT NULL,

    CONSTRAINT UQ_stories_account_id_num UNIQUE(account_id, num),
    CONSTRAINT FK_stories_account_id_accounts_id FOREIGN KEY(account_id)
        REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE TABLE pokes (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    story_id INTEGER NOT NULL,
    swf_id INT(10) NOT NULL,
    Nickname VARCHAR(25) NOT NULL,
    num SMALLINT(3) NOT NULL,
    xp INT(7) NOT NULL,
    lvl TINYINT(3) NOT NULL,
    move1 SMALLINT(3) NOT NULL,
    move2 SMALLINT(3) NOT NULL,
    move3 SMALLINT(3) NOT NULL,
    move4 SMALLINT(3) NOT NULL,
    targetingType TINYINT(1) NOT NULL,
    gender TINYINT(1) NOT NULL,
    pos INT(5) NOT NULL,
    extra TINYINT(1) NOT NULL,
    item TINYINT(2) NOT NULL,
    tag VARCHAR(2) NOT NULL,

    CONSTRAINT UQ_pokes_save_id_swf_id UNIQUE(story_id, swf_id),
    CONSTRAINT FK_pokes_story_id_stories_id FOREIGN KEY (story_id)
        REFERENCES stories (id) ON DELETE CASCADE
);

CREATE TABLE extras (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    story_id INTEGER NOT NULL,
    num TINYINT(2) NOT NULL,
    value INT(2) NOT NULL,

    CONSTRAINT FK_extras_story_id_stories_id FOREIGN KEY (story_id)
        REFERENCES stories (id) ON DELETE CASCADE
);

CREATE TABLE items (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    story_id INTEGER NOT NULL,
    num TINYINT(2) NOT NULL,
    value INT(2) NOT NULL,

    CONSTRAINT FK_items_story_id_stories_id FOREIGN KEY (story_id)
        REFERENCES stories (id) ON DELETE CASCADE
);

CREATE TABLE `1v1s` (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    num TINYINT(2) NOT NULL,
    money INT(5) NOT NULL,
    levelUnlocked TINYINT(2) NOT NULL,

    CONSTRAINT UQ_1v1s_account_id_num UNIQUE(account_id, num),
    CONSTRAINT FK_1v1s_account_id_accounts_id FOREIGN KEY(account_id)
        REFERENCES accounts (id) ON DELETE CASCADE
);