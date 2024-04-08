-- Potential advanced permission system
-- add privileges table (id and name)
-- add role_privileges table (id, role_id, privilege_id)
--   each row will be a privilege the role has
CREATE TABLE roles (
    id TINYINT(3) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO roles (id, name)
VALUES (1, 'Admin'), (100, 'User');

CREATE TABLE users (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    -- SWF max length of 50
    email VARCHAR(50) NOT NULL UNIQUE,
    role_id TINYINT(3) NOT NULL DEFAULT 100,
    -- SWF max length of 10
    -- But Bcrypt hashing has size of 72
    password VARCHAR(72) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT FK_users_role_id_roles_id FOREIGN KEY (role_id)
        REFERENCES roles(id)
);

CREATE TABLE sessions (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    -- ensure it is indeed 32 characters
    session_id VARCHAR(32) NOT NULL UNIQUE,
    data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);


CREATE TABLE settings (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    `key` VARCHAR(255) NOT NULL UNIQUE,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);