# CorridorPanel — macOS

Настольная панель для **MacBook Pro 16″**: тот же набор, что Android `corridor-panel` (SM10P).

Папка: [`macos/CorridorPanel/`](./).

Связано: [`docs/domofon_asterisk.md`](../../docs/domofon_asterisk.md), [`android/corridor-panel/`](../../android/corridor-panel/), [`ios/HomePanel/`](../../ios/HomePanel/).

## Возможности

| Колонка | Устройства |
|---|---|
| **Дверь** | ISAPI JPEG + SIP (Linphone, ext `102`) — звонок / ответ |
| **Свет** | коридор (`wb-mdm3_76` Ch1/K1), кладовка лента/свет (`239` K4/K5) |
| **Санузел** | ванна / туалет (`wb-mr6cv3_227` K1–K6) |
| **Тёплый пол** | коридор `15` · ванна `17` · туалет `16` |

Логический холст **1280×800** (`PanelFit`) — как Android SM10P; при ресайзе и fullscreen масштабируется с letterbox.

- Старт → **fullscreen**
- Красная/жёлтая кнопка окна, ⌘W или иконка в шапке → **в menu bar (трей)**, MQTT/SIP продолжают работать
- Клик по иконке двери в трее → Показать / Скрыть / Выйти
- Входящий SIP → баннер «Входящий звонок», звук, кнопки Принять/Отклонить; панель поднимается сама
- ⌃⌘F — fullscreen; настройки — ⌘, или шестерёнка

## Сборка

Требования: macOS 14+, Xcode 16+, [XcodeGen](https://github.com/yonaskolb/XcodeGen), сеть (CocoaMQTT + бинарники Linphone 5.5.39).

Linphone подключён как локальный `Vendor/linphonesw` (как iOS): удалённый git-URL с download.linphone.org часто не клонируется.

```bash
cd macos/CorridorPanel
xcodegen generate
open CorridorPanel.xcodeproj
```

В Xcode: Signing → Team (или Local). Запуск на Mac в домашней LAN.

```bash
# без подписи (проверка компиляции)
xcodebuild -project CorridorPanel.xcodeproj -scheme CorridorPanel \
  -configuration Debug -derivedDataPath ./DerivedData \
  build CODE_SIGNING_ALLOWED=NO
```

## Дефолты

| Параметр | Значение |
|----------|----------|
| MQTT | `192.168.1.1:1883` |
| SIP | `102@192.168.1.9` / `soft102test` / UDP |
| Дверь | ext `100`, камера `192.168.1.253` |

## Каталог устройств

Общий JSON: [`shared/corridor-panel/topics.json`](../../shared/corridor-panel/topics.json)  
Генерация: `python3 scripts/gen_corridor_topics.py`

## Архитектура

```
CorridorPanel (macOS)
  ├── MqttService  → CocoaMQTT → WB (+ stale > 120с)
  ├── SipService   → linphonesw → Asterisk → Hikvision
  └── DoorCamera   → RTSP sub / JPEG fallback (Digest)
```

## Структура

| Путь | Назначение |
|------|------------|
| `CorridorPanelApp.swift` | Точка входа, окно 1280×800 |
| `App/Session.swift` | MQTT + SIP |
| `Model/Topics.swift` | Как Android corridor-panel |
| `MQTT/MqttService.swift` | Брокер |
| `SIP/*` | Linphone + оверлей звонка |
| `UI/CorridorDashboard.swift` | 4 колонки |
| `UI/DoorCameraView.swift` | ISAPI превью (`scaledToFit`) |

`bundle id`: `ru.home.corridorpanel.macos`.
