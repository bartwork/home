-- name: ListRules :many
SELECT id, name, enabled, mode, cooldown_ms, graph_json, created_at, updated_at
FROM rules
ORDER BY id;

-- name: GetRule :one
SELECT id, name, enabled, mode, cooldown_ms, graph_json, created_at, updated_at
FROM rules
WHERE id = ?;

-- name: CreateRule :execlastid
INSERT INTO rules (name, enabled, mode, cooldown_ms, graph_json)
VALUES (?, ?, ?, ?, ?);

-- name: UpdateRule :execrows
UPDATE rules
SET name = ?,
    enabled = ?,
    mode = ?,
    cooldown_ms = ?,
    graph_json = ?,
    updated_at = datetime('now')
WHERE id = ?;

-- name: SetRuleEnabled :execrows
UPDATE rules
SET enabled = ?,
    updated_at = datetime('now')
WHERE id = ?;

-- name: DeleteRule :execrows
DELETE FROM rules
WHERE id = ?;
