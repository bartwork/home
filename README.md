# Мой дом

Локальный контроллер (Wirenboard): API + React UI + MQTT→WS + discovery.

## Быстрый старт

```bash
task deps
task run
```

http://localhost:9000 · `admin` / `admin`

При смене схемы: `rm -f data/home.db*` и перезапуск.

## Discovery

`MQTT_BROKER` (по умолчанию `tcp://127.0.0.1:1883`) → `/devices/#` → staging → UI **Обнаружено** → accept → виджеты.

## Taskfile

| Задача | Описание |
|--------|----------|
| `task run` | фронт + сервер |
| `task build` | бинарь `bin/home` |
| `task web:dev` | Vite, proxy `/api`+WS → `:9000` |
| `task generate` | sqlc + tg |
| `task test` | Go-тесты |
| `task smoke` | curl login |

## API

| Метод | Путь | Auth |
|-------|------|------|
| `POST` | `/api/v1/auth/login` | нет |
| `*` | `/api/v1/users` | JWT |
| `*` | `/api/v1/devices` | JWT |
| `*` | `/api/v1/discovered` | JWT |
| `WS` | `/api/v1/ws?token=` | JWT |
