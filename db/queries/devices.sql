-- name: ListDevices :many
SELECT id, name, mqtt_topic, device_type, unit, is_active, created_at
FROM devices
ORDER BY id;

-- name: GetDevice :one
SELECT id, name, mqtt_topic, device_type, unit, is_active, created_at
FROM devices
WHERE id = ?;

-- name: CreateDevice :execlastid
INSERT INTO devices (name, mqtt_topic, device_type, unit, is_active)
VALUES (?, ?, ?, ?, ?);

-- name: UpdateDevice :execrows
UPDATE devices
SET name = ?, mqtt_topic = ?, device_type = ?, unit = ?, is_active = ?
WHERE id = ?;

-- name: DeleteDevice :execrows
DELETE FROM devices
WHERE id = ?;
