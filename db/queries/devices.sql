-- name: ListDevices :many
SELECT
    d.id, d.name, d.mqtt_topic, d.device_type, d.unit, d.power_topic,
    d.room_id, COALESCE(r.name, '') AS room_name,
    d.icon, d.x, d.y, d.w, d.h,
    d.is_active, d.created_at, d.wb_device, d.wb_control, d.source
FROM devices d
LEFT JOIN rooms r ON r.id = d.room_id
ORDER BY COALESCE(r.sort_order, 9999), room_name, d.y, d.x, d.id;

-- name: GetDevice :one
SELECT
    d.id, d.name, d.mqtt_topic, d.device_type, d.unit, d.power_topic,
    d.room_id, COALESCE(r.name, '') AS room_name,
    d.icon, d.x, d.y, d.w, d.h,
    d.is_active, d.created_at, d.wb_device, d.wb_control, d.source
FROM devices d
LEFT JOIN rooms r ON r.id = d.room_id
WHERE d.id = ?;

-- name: GetDeviceByTopic :one
SELECT
    d.id, d.name, d.mqtt_topic, d.device_type, d.unit, d.power_topic,
    d.room_id, COALESCE(r.name, '') AS room_name,
    d.icon, d.x, d.y, d.w, d.h,
    d.is_active, d.created_at, d.wb_device, d.wb_control, d.source
FROM devices d
LEFT JOIN rooms r ON r.id = d.room_id
WHERE d.mqtt_topic = ?;

-- name: CreateDevice :execlastid
INSERT INTO devices (
    name, mqtt_topic, device_type, unit, power_topic, room_id, icon, x, y, w, h,
    is_active, wb_device, wb_control, source
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- name: UpdateDevice :execrows
UPDATE devices
SET name = ?, mqtt_topic = ?, device_type = ?, unit = ?, power_topic = ?,
    room_id = ?, icon = ?, x = ?, y = ?, w = ?, h = ?,
    is_active = ?, wb_device = ?, wb_control = ?
WHERE id = ?;

-- name: DeleteDevice :execrows
DELETE FROM devices
WHERE id = ?;
