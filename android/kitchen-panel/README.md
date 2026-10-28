# Kitchen Panel — Android для кухни (YC-SM41P)

Киоск-приложение 480×480: MQTT к Wiren Board. Отдельно от комнатной панели (`android/room-panel`).

## Вкладки

| Вкладка | Устройства |
|---|---|
| **Свет** | у окна / лампа / у двери (mdm3_218), бра, подсветка, вытяжка (mr6cv3_239) |
| **Пол** | тёплый пол кухни: реле K5, датчик m1w2_82 Ext.2, уставка/режим `floor_18_*` |
| **Климат** | кондиционер кухни ONOKOM `_1` |

Брокер по умолчанию: `192.168.1.1:1883` (двойной тап по активной вкладке → настройки).

## Сборка

```bash
cd android/kitchen-panel
./gradlew :app:installDebug
# или по Wi‑Fi:
adb connect <IP>:5555
adb -s <IP>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

`applicationId`: `ru.home.kitchenpanel` — можно ставить рядом с «Комната».

## Заметки

- id уставки пола `18` — id устройства в БД home (кабель 15 в docs ≠ id). При пересоздании БД проверьте `Topics.Floor.deviceId`.
- Уставка/режим публикуются сервисом `home` (`/devices/home/controls/floor_*`); реле и датчик — напрямую с WB.
- Топики: [`docs/mqtt_topics.md`](../../docs/mqtt_topics.md).
