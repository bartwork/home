-- name: ListRooms :many
SELECT id, name, x, y, w, h, sort_order
FROM rooms
ORDER BY sort_order, name, id;

-- name: GetRoom :one
SELECT id, name, x, y, w, h, sort_order
FROM rooms
WHERE id = ?;

-- name: GetRoomByName :one
SELECT id, name, x, y, w, h, sort_order
FROM rooms
WHERE name = ?;

-- name: CreateRoom :execlastid
INSERT INTO rooms (name, x, y, w, h, sort_order)
VALUES (?, ?, ?, ?, ?, ?);

-- name: UpdateRoom :execrows
UPDATE rooms
SET name = ?, x = ?, y = ?, w = ?, h = ?, sort_order = ?
WHERE id = ?;

-- name: DeleteRoom :execrows
DELETE FROM rooms
WHERE id = ?;
