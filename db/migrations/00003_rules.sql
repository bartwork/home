-- +goose Up
CREATE TABLE rules (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    enabled      INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
    mode         TEXT    NOT NULL DEFAULT 'single'
                     CHECK (mode IN ('single', 'restart', 'queued')),
    cooldown_ms  INTEGER NOT NULL DEFAULT 0,
    graph_json   TEXT    NOT NULL,
    created_at   TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at   TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_rules_enabled ON rules (enabled);

-- +goose Down
DROP INDEX IF EXISTS idx_rules_enabled;
DROP TABLE IF EXISTS rules;
