# Мой дом

Локальный контроллер (Wirenboard): API + React UI + MQTT→WS.

## Быстрый старт

```bash
task deps
task tg:install
task generate
task run
```

Открыть http://localhost:9000 · логин `admin` / пароль `admin`

Если схема БД менялась — удали `data/home.db` и перезапусти (сид создаст admin заново).

## Taskfile

| Задача | Описание |
|--------|----------|
| `task run` | фронт + сервер |
| `task build` | бинарь `bin/home` |
| `task web:dev` | Vite с proxy `/api` и WS → `:9000` |
| `task generate` | sqlc + tg |
| `task test` | Go-тесты |
| `task smoke` | curl login |

## API

| Метод | Путь | Auth |
|-------|------|------|
| `POST` | `/api/v1/auth/login` | нет |
| `*` | `/api/v1/users` | JWT |
| `*` | `/api/v1/devices` | JWT |
| `WS` | `/api/v1/ws?token=` | JWT |

MQTT: `MQTT_BROKER` (по умолчанию `tcp://127.0.0.1:1883`).
