-- name: ListDevices :many
SELECT id, name, mqtt_topic, device_type, unit, is_active, created_at, wb_device, wb_control, source
FROM devices
ORDER BY wb_device, id;

-- name: GetDevice :one
SELECT id, name, mqtt_topic, device_type, unit, is_active, created_at, wb_device, wb_control, source
FROM devices
WHERE id = ?;

-- name: GetDeviceByTopic :one
SELECT id, name, mqtt_topic, device_type, unit, is_active, created_at, wb_device, wb_control, source
FROM devices
WHERE mqtt_topic = ?;

-- name: CreateDevice :execlastid
INSERT INTO devices (name, mqtt_topic, device_type, unit, is_active, wb_device, wb_control, source)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- name: UpdateDevice :execrows
UPDATE devices
SET name = ?, mqtt_topic = ?, device_type = ?, unit = ?, is_active = ?, wb_device = ?, wb_control = ?
WHERE id = ?;

-- name: DeleteDevice :execrows
DELETE FROM devices
WHERE id = ?;
