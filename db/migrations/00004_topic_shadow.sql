-- +goose Up
CREATE TABLE topic_shadow (
    topic      TEXT    NOT NULL PRIMARY KEY,
    payload    TEXT    NOT NULL DEFAULT '',
    updated_at TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- +goose Down
DROP TABLE IF EXISTS topic_shadow;
