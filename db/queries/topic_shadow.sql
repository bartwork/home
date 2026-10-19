-- name: ListTopicShadow :many
SELECT topic, payload, updated_at
FROM topic_shadow
ORDER BY topic;

-- name: UpsertTopicShadow :exec
INSERT INTO topic_shadow (topic, payload, updated_at)
VALUES (?, ?, datetime('now'))
ON CONFLICT(topic) DO UPDATE SET
    payload = excluded.payload,
    updated_at = datetime('now');

-- name: GetTopicShadow :one
SELECT topic, payload, updated_at
FROM topic_shadow
WHERE topic = ?;
