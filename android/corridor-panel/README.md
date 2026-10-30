# Corridor Panel — Android для коридора (YC-SM10P)

Киоск под **горизонтальную** панель **YC-SM10P** 10.1″ (1280×800, landscape).  
Кухня/комната — квадратные SM08P / YC-SM41P (`android/kitchen-panel`, `android/room-panel`).

См. [`docs/rozetki_vyklyuchateli.md`](../../docs/rozetki_vyklyuchateli.md) (SM10P в коридоре).

## UI

Один экран, четыре колонки:

| Колонка | Устройства |
|---|---|
| **Дверь** | превью ISAPI JPEG + SIP (Linphone, ext `102`) — звонок / ответ |
| **Свет** | коридор (`wb-mdm3_76` Ch1/K1), кладовка **лента**/свет (`239` K4/K5) |
| **Санузел** | две колонки: ванна / туалет (`wb-mr6cv3_227` K1–K6) |
| **Тёплый пол** | вкладки: коридор `floor_15_*` · ванна `17` · туалет `16` |

**Не на этой панели** (по каталогу): кондиционеры (кухня/комната), RGB-лента карниза (`wb-led_221` — комната).

Шапка: двойной тап → настройки MQTT/SIP/яркости; удержание → погасить экран.  
Входящий звонок будит экран и открывает полноэкранный оверлей (принять / отклонить / mute).

Ориентация зафиксирована `sensorLandscape`. Логический холст `PanelFit` — 1280×800.

Брокер по умолчанию: `192.168.1.1:1883`.  
SIP по умолчанию: `102@192.168.1.9` (см. [`docs/domofon_asterisk.md`](../../docs/domofon_asterisk.md)).

## Сборка

```bash
cd android/corridor-panel
./gradlew :app:installDebug
# Wi‑Fi:
adb connect <IP>:5555
adb -s <IP>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <IP>:5555 shell am start -n ru.home.corridorpanel/.MainActivity
```

`applicationId`: `ru.home.corridorpanel`.

## Заметки

- id полов seed: коридор `15`, туалет `16`, ванна `17`, кухня `18`. При другой БД проверьте `Topics.floors`.
- Топики: [`docs/mqtt_topics.md`](../../docs/mqtt_topics.md).
