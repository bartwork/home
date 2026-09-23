# home

HTTP API пользователей (tg + SQLite/SQLCipher).

```bash
CGO_ENABLED=1 go run ./cmd/home
```

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/v1/users` | список |
| `GET` | `/api/v1/users/:id` | один |
| `POST` | `/api/v1/users` | создать |
| `PUT` | `/api/v1/users/:id` | обновить |
| `DELETE` | `/api/v1/users/:id` | удалить |

```bash
sqlc generate -f db/sqlc.yaml
go generate ./contracts
```
