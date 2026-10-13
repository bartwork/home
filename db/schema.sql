CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    login         TEXT    NOT NULL UNIQUE,
    last_name     TEXT    NOT NULL DEFAULT '',
    first_name    TEXT    NOT NULL DEFAULT '',
    second_name   TEXT    NOT NULL DEFAULT '',
    email         TEXT    NOT NULL UNIQUE,
    phone         TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL DEFAULT '',
    last_auth_at  TEXT,
    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at    TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE devices (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    mqtt_topic  TEXT    NOT NULL UNIQUE,
    device_type TEXT    NOT NULL,
    unit        TEXT    NOT NULL DEFAULT '',
    is_active   INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    wb_device   TEXT    NOT NULL DEFAULT '',
    wb_control  TEXT    NOT NULL DEFAULT '',
    source      TEXT    NOT NULL DEFAULT 'manual'
);

CREATE INDEX idx_devices_type ON devices (device_type);

CREATE TABLE discovered_devices (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mqtt_topic      TEXT    NOT NULL UNIQUE,
    wb_device       TEXT    NOT NULL DEFAULT '',
    wb_control      TEXT    NOT NULL DEFAULT '',
    title           TEXT    NOT NULL DEFAULT '',
    suggested_type  TEXT    NOT NULL DEFAULT '',
    unit            TEXT    NOT NULL DEFAULT '',
    last_payload    TEXT    NOT NULL DEFAULT '',
    last_seen_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    status          TEXT    NOT NULL DEFAULT 'new'
                        CHECK (status IN ('new', 'accepted', 'ignored')),
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_discovered_status ON discovered_devices (status);
CREATE INDEX idx_discovered_type ON discovered_devices (suggested_type);
