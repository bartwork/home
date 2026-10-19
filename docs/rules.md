# Движок правил

Graph-runtime в процессе `home` (без wb-rules / Node-RED / HA). IR = React Flow `nodes`/`edges`. UI: `/rules`.

## Пакеты

| Пакет | Роль |
|-------|------|
| `internal/rules` | compile, eval, cron, schedule, HTTP |
| `internal/floor` | топики home, graph-шаблон, ensure, bootstrap |
| `internal/mqttbridge` | MQTT + local echo/dedupe для `/devices/home/*` |

## API

JWT на `/api/*`.

| Метод | Путь |
|-------|------|
| GET/POST | `/api/v1/rules` |
| GET/PUT/DELETE | `/api/v1/rules/:id` |
| POST | `/api/v1/rules/:id/enable\|disable\|run` |
| POST | `/api/v1/rules/ensure-floors` |

`mode`: `single` \| `restart` \| `queued`.

## Узлы

`trigger.mqtt` · `trigger.cron` · `trigger.manual` · `condition.compare|and|or` · `action.mqtt|delay|set_var` · `logic.thermostat` (`temp`, `setpoint`, `mode`, `hysteresis`, `relay`).

Condition-ветки: `sourceHandle` `true`/`false`.

## Тёплый пол

Топики (`internal/floor`):

- `/devices/home/controls/floor_{id}_setpoint`
- `/devices/home/controls/floor_{id}_mode` — `off` \| `heat` \| `auto`

Persisted в `topic_shadow`. Bridge делает один local apply + suppress broker loopback.

`FloorWidget` пишет те же топики (работает и при MQTT offline, если есть WS).

Старт: `floor.Bootstrap` → ensure rules (`meta.floorDeviceId`) → seed defaults `26`/`auto` → `SyncThermostats` (реле сразу).

Автоправила: `cooldownMs: 0`.

## React Flow

- `/rules` — список
- `/rules/:id` — редактор `@xyflow/react`

## Защиты

- только state-топики
- `change` / topic-index O(1) lookup
- self-echo правил ~1s
- `/devices/home/*` не в discovery
