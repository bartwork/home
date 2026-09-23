-- name: ListUsers :many
SELECT
    id,
    last_name,
    first_name,
    second_name,
    email,
    phone,
    last_auth_at,
    is_active
FROM users
ORDER BY id;

-- name: GetUser :one
SELECT
    id,
    last_name,
    first_name,
    second_name,
    email,
    phone,
    last_auth_at,
    is_active
FROM users
WHERE id = ?;

-- name: CreateUser :execlastid
INSERT INTO users (
    last_name,
    first_name,
    second_name,
    email,
    phone,
    password_hash,
    is_active
) VALUES (?, ?, ?, ?, ?, ?, ?);

-- name: UpdateUser :execrows
UPDATE users
SET
    last_name = ?,
    first_name = ?,
    second_name = ?,
    email = ?,
    phone = ?,
    is_active = ?
WHERE id = ?;

-- name: SetUserPassword :execrows
UPDATE users
SET password_hash = ?
WHERE id = ?;

-- name: DeleteUser :execrows
DELETE FROM users
WHERE id = ?;

-- name: GetUserForAuth :one
SELECT
    id,
    last_name,
    first_name,
    second_name,
    email,
    phone,
    password_hash,
    last_auth_at,
    is_active
FROM users
WHERE email = ? OR phone = ?
LIMIT 1;

-- name: TouchLastAuth :execrows
UPDATE users
SET last_auth_at = ?
WHERE id = ?;
