-- name: ListDiscoveredNew :many
SELECT
    id, mqtt_topic, wb_device, wb_control, title, suggested_type, unit,
    last_payload, last_seen_at, status, created_at
FROM discovered_devices
WHERE status = 'new'
ORDER BY last_seen_at DESC, id DESC;

-- name: GetDiscovered :one
SELECT
    id, mqtt_topic, wb_device, wb_control, title, suggested_type, unit,
    last_payload, last_seen_at, status, created_at
FROM discovered_devices
WHERE id = ?;

-- name: UpsertDiscoveredState :exec
INSERT INTO discovered_devices (
    mqtt_topic, wb_device, wb_control, title, suggested_type, unit, last_payload, last_seen_at, status
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'new')
ON CONFLICT(mqtt_topic) DO UPDATE SET
    last_payload = excluded.last_payload,
    last_seen_at = excluded.last_seen_at,
    title = CASE
        WHEN discovered_devices.title = '' THEN excluded.title
        ELSE discovered_devices.title
    END,
    suggested_type = CASE
        WHEN excluded.suggested_type != '' THEN excluded.suggested_type
        ELSE discovered_devices.suggested_type
    END,
    unit = CASE
        WHEN excluded.unit != '' THEN excluded.unit
        ELSE discovered_devices.unit
    END,
    status = CASE
        WHEN discovered_devices.status = 'ignored' THEN 'ignored'
        ELSE 'new'
    END;

-- name: UpsertDiscoveredMeta :exec
INSERT INTO discovered_devices (
    mqtt_topic, wb_device, wb_control, title, suggested_type, unit, last_payload, last_seen_at, status
) VALUES (?, ?, ?, ?, ?, ?, '', ?, 'new')
ON CONFLICT(mqtt_topic) DO UPDATE SET
    last_seen_at = excluded.last_seen_at,
    title = CASE
        WHEN excluded.title != '' THEN excluded.title
        ELSE discovered_devices.title
    END,
    suggested_type = CASE
        WHEN excluded.suggested_type != '' THEN excluded.suggested_type
        ELSE discovered_devices.suggested_type
    END,
    unit = CASE
        WHEN excluded.unit != '' THEN excluded.unit
        ELSE discovered_devices.unit
    END,
    status = CASE
        WHEN discovered_devices.status = 'ignored' THEN 'ignored'
        ELSE 'new'
    END;

-- name: SetDiscoveredStatus :execrows
UPDATE discovered_devices
SET status = ?
WHERE id = ?;

-- name: CountDiscoveredNew :one
SELECT COUNT(*) FROM discovered_devices WHERE status = 'new';

-- name: ListDiscoveredNewByType :many
SELECT
    id, mqtt_topic, wb_device, wb_control, title, suggested_type, unit,
    last_payload, last_seen_at, status, created_at
FROM discovered_devices
WHERE status = 'new' AND suggested_type = ?
ORDER BY id;
