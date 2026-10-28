# Яндекс Алиса на Wiren Board — как настраивать

Инструкция для агента/человека: как массово добавлять и править устройства в интеграции Алисы на контроллере, **без** кликов по каждому в UI.

Связанные файлы:

- список устройств: [`alice_devices.md`](alice_devices.md)
- MQTT-каталог квартиры: [`mqtt_topics.md`](mqtt_topics.md)
- официальная wiki: [Yandex Smart Home](https://wiki.wirenboard.com/wiki/Yandex-smart-home)

## Где живёт

| Что | Значение |
|---|---|
| UI | `http://192.168.1.1/#/integrations/alice` |
| Кабинет привязки | https://alice.wirenboard.com/ |
| Конфиг на WB | `/etc/wb-mqtt-alice-devices.conf` |
| Клиент | `wb-mqtt-alice-client` (онлайн только если привязан контроллер **и** есть ≥1 устройство) |
| Конфигуратор | `wb-mqtt-alice-config` |

Язык веб-интерфейса контроллера должен быть **русский**, иначе пункт «Алиса» не работает как надо.

## Важно про «Офлайн»

После привязки контроллер на alice.wirenboard.com часто висит **Offline**, пока:

1. в конфигураторе есть хотя бы одно устройство;
2. в приложении «Дом с Алисой» добавлен навык **Wiren Board**.

«Обновить список устройств» само по себе MQTT-устройства WB не подтягивает — только то, что создано в конфигураторе.

## API (предпочтительно)

UI ходит в REST на том же origin контроллера. С LAN (без авторизации) обычно отвечает `GET/POST /api/integrations/alice*` напрямую; если 401 — сессия браузера (cookie после логина) и `fetch(..., { credentials: 'include' })`.

Методы:

| Метод | Путь | Назначение |
|---|---|---|
| `GET` | `/api/integrations/alice` | весь конфиг: `rooms` + `devices` |
| `POST` | `/api/integrations/alice/room` | тело `{ "name": "Кухня" }` |
| `PUT` | `/api/integrations/alice/room/{id}` | переименовать комнату |
| `DELETE` | `/api/integrations/alice/room/{id}` | удалить комнату |
| `POST` | `/api/integrations/alice/device` | создать устройство (см. тело ниже) |
| `PUT` | `/api/integrations/alice/device/{id}` | обновить устройство |
| `DELETE` | `/api/integrations/alice/device/{id}` | удалить |
| `POST` | `/api/integrations/alice/enable_client` | `{ "enabled": true }` |
| `GET` | `/api/integrations/alice/link_status` | статус привязки |
| `PUT` | `/api/integrations/alice/unlink_controller` | отвязать |

Ответ `POST /device` — объект `{ "<new-id>": { ...device } }` (id — ключ).  
`PUT /device/{id}` — само устройство без обёртки.

### Формат MQTT в умениях

**Не** полный топик `/devices/.../controls/...`, а кратко:

```text
{mqttDeviceId}/{controlName}
```

Примеры: `wb-mdm3_75/K1`, `wb-mdm3_75/Channel 1`, `wb-led_221/RGB Palette`.

### Шаблоны capabilities

**Реле / switch (только вкл/выкл):**

```json
{
  "type": "devices.capabilities.on_off",
  "mqtt": "wb-mr6cv3_227/K1",
  "parameters": { "split": false, "instance": "on" },
  "retrievable": true,
  "reportable": true
}
```

**Диммер MDM3 (канал N):** `on_off` → `KN`, яркость → `Channel N` (0…100):

```json
[
  {
    "type": "devices.capabilities.on_off",
    "mqtt": "wb-mdm3_75/K1",
    "parameters": { "split": false, "instance": "on" },
    "retrievable": true,
    "reportable": true
  },
  {
    "type": "devices.capabilities.range",
    "mqtt": "wb-mdm3_75/Channel 1",
    "parameters": {
      "instance": "brightness",
      "range": { "min": 0, "max": 100, "precision": 1 },
      "unit": "unit.percent"
    },
    "retrievable": true,
    "reportable": true
  }
]
```

**RGB-лента WB-LED:** вкл + яркость + цвет (`RGB Palette`, модель `rgb`):

```json
[
  {
    "type": "devices.capabilities.on_off",
    "mqtt": "wb-led_221/RGB Strip",
    "parameters": { "split": false, "instance": "on" },
    "retrievable": true,
    "reportable": true
  },
  {
    "type": "devices.capabilities.range",
    "mqtt": "wb-led_221/RGB Strip Brightness",
    "parameters": {
      "instance": "brightness",
      "range": { "min": 0, "max": 100, "precision": 1 },
      "unit": "unit.percent"
    },
    "retrievable": true,
    "reportable": true
  },
  {
    "type": "devices.capabilities.color_setting",
    "mqtt": "wb-led_221/RGB Palette",
    "parameters": { "color_model": "rgb", "instance": "rgb" },
    "retrievable": true,
    "reportable": true
  }
]
```

Тип устройства: `devices.types.light.strip`.

**Тёплый пол** (реле + датчик температуры, без уставки):

```json
{
  "name": "Тёплый пол",
  "type": "devices.types.thermostat",
  "capabilities": [
    {
      "type": "devices.capabilities.on_off",
      "mqtt": "wb-mr6cv3_225/K1",
      "parameters": { "split": false, "instance": "on" },
      "retrievable": true,
      "reportable": true
    }
  ],
  "properties": [
    {
      "type": "devices.properties.float",
      "mqtt": "wb-m1w2_83/External Sensor 1",
      "parameters": { "instance": "temperature", "unit": "°C" },
      "retrievable": true,
      "reportable": true
    }
  ]
}
```

Не добавлять `range`/`temperature` как умение — у нас нет уставки, только вкл/выкл. Единица свойства float в WB — `°C` (не `unit.temperature.celsius`).

**Кондиционер ONOKOM** (`devices.types.thermostat.ac`) — у режима обязательно `mqtt_value_match` (число из MQTT):

```json
{
  "name": "Кондиционер",
  "type": "devices.types.thermostat.ac",
  "capabilities": [
    {
      "type": "devices.capabilities.on_off",
      "mqtt": "ONOKOM-AIR-HR-1-MB-B_1/Active",
      "parameters": { "split": false, "instance": "on" },
      "retrievable": true,
      "reportable": true
    },
    {
      "type": "devices.capabilities.range",
      "mqtt": "ONOKOM-AIR-HR-1-MB-B_1/Target temperature",
      "parameters": {
        "instance": "temperature",
        "random_access": true,
        "range": { "min": 16, "max": 30, "precision": 1 },
        "unit": "unit.temperature.celsius"
      },
      "retrievable": true,
      "reportable": true
    },
    {
      "type": "devices.capabilities.mode",
      "mqtt": "ONOKOM-AIR-HR-1-MB-B_1/Mode",
      "parameters": {
        "instance": "thermostat",
        "modes": [
          { "value": "heat", "mqtt_value_match": "1" },
          { "value": "cool", "mqtt_value_match": "2" },
          { "value": "auto", "mqtt_value_match": "3" },
          { "value": "dry", "mqtt_value_match": "4" },
          { "value": "fan_only", "mqtt_value_match": "5" }
        ]
      },
      "retrievable": true,
      "reportable": true
    }
  ],
  "properties": [
    {
      "type": "devices.properties.float",
      "mqtt": "ONOKOM-AIR-HR-1-MB-B_1/Indoor air temperature",
      "parameters": { "instance": "temperature", "unit": "°C" },
      "retrievable": true,
      "reportable": true
    }
  ]
}
```

Полный набор на контроллере (ещё `fan_speed` + `swing`) — см. таблицу маппинга в [`alice_devices.md`](alice_devices.md) § «Кондиционеры ONOKOM».

**Белый канал WB-LED (Channel 4)** — **отдельное** устройство (Алиса плохо мешает RGBW в одном):

```json
{
  "name": "Белый карниз",
  "type": "devices.types.light",
  "capabilities": [
    {
      "type": "devices.capabilities.on_off",
      "mqtt": "wb-led_221/Channel 4",
      "parameters": { "split": false, "instance": "on" },
      "retrievable": true,
      "reportable": true
    },
    {
      "type": "devices.capabilities.range",
      "mqtt": "wb-led_221/Channel 4 Brightness",
      "parameters": {
        "instance": "brightness",
        "range": { "min": 0, "max": 100, "precision": 1 },
        "unit": "unit.percent"
      },
      "retrievable": true,
      "reportable": true
    }
  ]
}
```

### Тело POST `/device`

```json
{
  "name": "Основной свет",
  "status_info": null,
  "description": null,
  "room_id": "<uuid комнаты из GET>",
  "type": "devices.types.light",
  "capabilities": [ /* ... */ ],
  "properties": []
}
```

Частые `type`: `devices.types.light`, `devices.types.light.lamp`, `devices.types.light.strip`, `devices.types.thermostat`, `devices.types.thermostat.ac`.

## Как делать из Cursor (агент)

1. API на LAN обычно открыт без cookie: `curl http://192.168.1.1/api/integrations/alice`. Иначе — browser MCP на UI с сессией.
2. Сопоставить комнаты по `rooms[*].name` → `room_id`.
3. Создать/обновить устройства `POST`/`PUT` с шаблонами выше; топики брать из [`mqtt_topics.md`](mqtt_topics.md) / [`alice_devices.md`](alice_devices.md).
4. После изменений обновить [`alice_devices.md`](alice_devices.md).
5. Пользователю: в приложении «Дом с Алисой» → Wiren Board → **Обновить список устройств**.

Ручное правление `/etc/wb-mqtt-alice-devices.conf` возможно, но UI/сервис могут перезаписать файл; перед правкой останавливать `wb-mqtt-alice-config` и `wb-mqtt-alice-client`. Предпочтительнее API.

## Конвенции этой квартиры

- Комнаты Алисы: Коридор, Зал, Лоджия, Кухня, Кладовка, Ванна, Туалет (не «комната» — в Алисе зал = `Зал`).
- В конфигуратор Алисы: **свет**, **тёплые полы**, **кондиционеры**; вытяжки, MSW — отдельно по запросу.
- Имена могут повторяться в разных комнатах («Свет», «Зеркало», «Тёплый пол», «Кондиционер») — Алиса различает по комнате.
- Имена в Алисе ≠ имена в MQTT-каталоге: зал в Алисе = `Зал` (в mqtt — «комната»); бра зала в Алисе = `Бра левое` / `Бра правое` (в mqtt — «около кладовки» / «около лоджии»).
- Источник правды по каналам MQTT: [`mqtt_topics.md`](mqtt_topics.md); проверенные модули: кладовка/кухня реле = `239`, ванна/туалет = `227`, полы = `225` + `m1w2_82/83`, кондеи = `ONOKOM-AIR-…_1` (кухня) / `_2` (зал).

## После смены IP / шины

Контроллер сейчас: **`192.168.1.1`**. Все прежние Modbus на `ttyRS485-1`. Slave id / MQTT device id при переносе на одну шину **не менялись**.
