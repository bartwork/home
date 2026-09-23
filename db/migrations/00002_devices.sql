-- +goose Up
CREATE TABLE devices (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    mqtt_topic  TEXT    NOT NULL UNIQUE,
    device_type TEXT    NOT NULL,
    unit        TEXT    NOT NULL DEFAULT '',
    is_active   INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_devices_type ON devices (device_type);

-- +goose Down
DROP INDEX IF EXISTS idx_devices_type;
DROP TABLE IF EXISTS devices;
