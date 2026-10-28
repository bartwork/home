# Zigbee → WB: русские title контролов

Снимок MQTT meta виртуальных устройств (`driver: wb-rules`) на контроллере **192.168.1.1**.

Данные: [`zigbee_virtual_devices_meta.json`](./zigbee_virtual_devices_meta.json).

Подпись в UI берётся из retained-топика:

```text
/devices/<имя устройства>/controls/<канал>/meta
```

поле `title.ru` / `title.en`. Это не файл на диске.

## Устройства

| MQTT device id | Назначение |
|---|---|
| `Беспроводной датчик протечки кухни` | протечка (Zigbee) |
| `Уличный датчик температуры` | уличная температура / влажность |

## Маппинг title (протечка кухни)

| Канал | en | ru |
|---|---|---|
| `battery` | Battery | Батарея |
| `battery_low` | Battery low | Низкий заряд |
| `device_temperature` | Device temperature | Температура устройства |
| `last_seen` | Last seen | Последний раз на связи |
| `linkquality` | Link quality | Качество связи |
| `power_outage_count` | Power outage count | Счётчик отключений питания |
| `state` | State | Состояние |
| `tamper` | Tamper | Вскрытие корпуса |
| `voltage` | Voltage | Напряжение |
| `water_leak` | Water leak | Протечка |

## Восстановление

Публиковать **retained** JSON из `controls` в топик meta. Пример для одного канала (`mosquitto_pub` с машины в LAN):

```bash
DEV='Беспроводной датчик протечки кухни'
CTRL=device_temperature
mosquitto_pub -h 192.168.1.1 -r -t "/devices/${DEV}/controls/${CTRL}/meta" \
  -m '{"order":3,"readonly":true,"type":"text","title":{"en":"Device temperature","ru":"Температура устройства"}}'
```

Или прогнать весь JSON скриптом: для каждого `devices[id].controls[ctrl]` →  
`/devices/{id}/controls/{ctrl}/meta` с `-r`.

Если контролы заново создаёт wb-rules без `title`, после рестарта правила снимок нужно применить снова (или добавить title в само правило).
