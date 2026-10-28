# Room Panel — Android для комнаты (YC-SM41P)

Киоск-приложение под панель 480×480: MQTT напрямую к Wiren Board, без веб-бэкенда.

## Управление

| Вкладка | Устройства |
|---|---|
| **Свет** | основной, доп., лоджия, бра (кладовка/лоджия), кладовка |
| **Лента** | RGB Strip + яркость/H/S, белый канал, авто-оттенок, палитра |
| **Климат** | кондиционер комнаты (ONOKOM `_2`): вкл, °C, режим, вент, качание, Eco/Сон |

Топики — как в [`docs/mqtt_topics.md`](../../docs/mqtt_topics.md).  
Команды публикуются в `…/controls/…/on` (формат WB).

Брокер по умолчанию: `192.168.1.1:1883` (тап по статусу MQTT → настройки).

## Сборка

Нужны Android Studio (Ladybug+) или SDK + JDK 17.

```bash
cd android/room-panel
# один раз: создать local.properties с sdk.dir=...
# или открыть папку в Android Studio — Sync / Run
```

Установка на панель по USB/ADB:

```bash
./gradlew :app:installDebug
# или
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### По Wi‑Fi (wireless debugging)

На планшете: **Настройки → Для разработчиков → Беспроводная отладка** (Android 11+).

1. Включить беспроводную отладку, открыть «Пара устройств» / pairing.
2. С Mac (один раз на пару):

```bash
adb pair <IP_ПЛАНШЕТА>:<ПОРТ_ПАРИНГА>
# ввести код с экрана планшета
adb connect <IP_ПЛАНШЕТА>:<ПОРТ_ОТЛАДКИ>
adb devices   # должен появиться IP:port  device
```

3. Установка (если одновременно висит эмулятор — укажи serial):

```bash
cd android/room-panel
./gradlew :app:installDebug
# или явно:
adb -s <IP_ПЛАНШЕТА>:<ПОРТ> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <IP_ПЛАНШЕТА>:<ПОРТ> shell am start -n ru.home.roompanel/.MainActivity
```

Альтернатива без pairing (старый способ): один раз по USB  
`adb tcpip 5555`, отключить кабель, затем `adb connect <IP>:5555`.

На YC-SM41P удобно поставить как launcher (в манифесте есть `HOME`) или держать в Fully Kiosk / как единственное приложение.

## Поведение

- экран не гаснет (`KEEP_SCREEN_ON`);
- системные панели скрыты (свайп сверху/снизу — временно);
- cleartext TCP к MQTT в LAN разрешён;
- состояние с retained-топиков WB, UI обновляется по подписке.
