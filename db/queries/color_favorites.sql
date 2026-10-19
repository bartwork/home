-- name: ListColorFavorites :many
SELECT id, hue, saturation, brightness, sort_order, created_at
FROM color_favorites
ORDER BY sort_order, id;

-- name: GetColorFavorite :one
SELECT id, hue, saturation, brightness, sort_order, created_at
FROM color_favorites
WHERE id = ?;

-- name: CountColorFavorites :one
SELECT COUNT(*) FROM color_favorites;

-- name: CreateColorFavorite :execlastid
INSERT INTO color_favorites (hue, saturation, brightness, sort_order)
VALUES (?, ?, ?, ?);

-- name: DeleteColorFavorite :execrows
DELETE FROM color_favorites
WHERE id = ?;

-- name: DeleteAllColorFavorites :exec
DELETE FROM color_favorites;
