-- +goose Up
CREATE TABLE color_favorites (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    hue        INTEGER NOT NULL CHECK (hue BETWEEN 0 AND 360),
    saturation INTEGER NOT NULL CHECK (saturation BETWEEN 0 AND 100),
    brightness INTEGER NOT NULL CHECK (brightness BETWEEN 0 AND 100),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE UNIQUE INDEX idx_color_favorites_hsv
    ON color_favorites (hue, saturation, brightness);

CREATE INDEX idx_color_favorites_sort
    ON color_favorites (sort_order, id);

-- +goose Down
DROP INDEX IF EXISTS idx_color_favorites_sort;
DROP INDEX IF EXISTS idx_color_favorites_hsv;
DROP TABLE IF EXISTS color_favorites;
