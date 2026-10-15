# Мой дом

Локальный контроллер (Wirenboard): API + React UI + MQTT→WS + discovery.

## Быстрый старт

```bash
task deps
MQTT_BROKER=tcp://192.168.1.9:1883 task run
```

http://localhost:9000 · в **dev** первый пользователь: `admin` / `admin`

Если `:9000` занят (часто Docker):

```bash
BIND_SERVICE=:9020 MQTT_BROKER=tcp://192.168.1.9:1883 task run
```

→ http://localhost:9020

При смене схемы БД: `rm -f data/home.db*` и перезапуск.

Не запускай два `home` сразу — MQTT client id уникален по PID, но два подписчика на один брокер всё равно путают отладку.

## Секреты

| Env | Dev | Prod (`APP_ENV=prod`) |
|-----|-----|------------------------|
| `JWT_SECRET` | дефолт для разработки | **обязателен**, не дефолт |
| `SQLCIPHER_KEY` | дефолт для разработки | **обязателен**, не дефолт |
| `ADMIN_PASSWORD` | иначе `admin` при пустой БД | **обязателен** при пустой БД, не `admin` |

```bash
APP_ENV=prod \
JWT_SECRET='…' \
SQLCIPHER_KEY='…' \
ADMIN_PASSWORD='…' \
MQTT_BROKER=tcp://192.168.1.9:1883 \
./bin/home
```

`SQLCIPHER_KEY` должен совпадать с ключом, которым создан `data/home.db`. Dev-ключ в prod запрещён: либо тот же ключ, что при создании файла (если он не дефолтный), либо новая БД (`rm -f data/home.db*`).

## Discovery

`MQTT_BROKER` → `/devices/#` → staging → UI **Обнаружено** → accept → виджеты.  
Команды пишутся в `…/controls/X/on` (конвенция Wiren Board).

## Taskfile

| Задача | Описание |
|--------|----------|
| `task run` | фронт + сервер |
| `task build` | бинарь `bin/home` |
| `task web:dev` | Vite, proxy `/api`+WS → `HOME_API` (по умолчанию `:9000`) |
| `task generate` | sqlc + tg |
| `task test` | Go-тесты |
| `task smoke` | curl login |

Vite на другой порт API:

```bash
HOME_API=http://127.0.0.1:9020 task web:dev
```

## API

| Метод | Путь | Auth |
|-------|------|------|
| `POST` | `/api/v1/auth/login` | нет |
| `*` | `/api/v1/users` | JWT |
| `*` | `/api/v1/devices` | JWT |
| `*` | `/api/v1/discovered` | JWT |
| `WS` | `/api/v1/ws?token=` | JWT |
