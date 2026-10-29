# iOS HomePanel — SIP + MQTT

Приложение в [`ios/HomePanel/`](../ios/HomePanel/): управление светом по MQTT и видеозвонок с домофоном через Linphone SDK / Asterisk.

Связано: [`domofon_asterisk.md`](domofon_asterisk.md), Android-панели `android/kitchen-panel`, `android/room-panel`.

## Возможности MVP

- **Свет** — кухня + комната (диммеры и реле), топики как у Android `Topics`
- **Домофон** — регистрация SIP, исходящий на ext двери, входящий с видео, DTMF
- **Настройки** — MQTT host/port, SIP domain/user/pass/transport

## Сборка

Требования: macOS, Xcode 16+, сеть для скачивания XCFramework Linphone (первый resolve).

```bash
cd ios/HomePanel
xcodegen generate          # после правок project.yml
open HomePanel.xcodeproj
```

В Xcode: Signing → Team. Запуск на **реальном iPhone** в домашней LAN (симулятор: MQTT ок, SIP/камера ограничены).

Зависимости SPM:

- [CocoaMQTT](https://github.com/emqx/CocoaMQTT)
- Linphone SDK 5.5.39 — локальный пакет [`Vendor/linphonesw`](../ios/HomePanel/Vendor/linphonesw) (бинари с `download.linphone.org`)

```bash
# проверка сборки без подписи
xcodebuild -project HomePanel.xcodeproj -scheme HomePanel \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build CODE_SIGNING_ALLOWED=NO
```

## Дефолтные настройки

| Параметр | Значение |
|----------|----------|
| MQTT | `192.168.1.1:1883` |
| SIP user | `102` |
| SIP password | `soft102test` |
| SIP domain | `192.168.1.9` (Asterisk на Mac; после переноса на WB — IP контроллера) |
| Transport | UDP |
| Домофон ext | `100` |

Dialplan Asterisk уже звонит на `101&102` — iOS на `102` получает входящие параллельно с Linphone Mac.

## Архитектура

```
HomePanel (iOS)
  ├── MqttService  → CocoaMQTT → WB 192.168.1.1
  └── SipService   → linphonesw → Asterisk → Hikvision 100
```

Команды света: publish в `stateTopic + "/on"` (Wiren Board).

## Структура кода

| Путь | Назначение |
|------|------------|
| `App/HomePanelApp.swift` | Точка входа |
| `App/HomeSession.swift` | Сессия: settings + MQTT + SIP, reconnect |
| `Model/Topics.swift` | Каталог MQTT света |
| `Model/AppSettings.swift` | UserDefaults + Defaults |
| `MQTT/MqttService.swift` | Брокер + toggle/dimmer (delegate bridge) |
| `SIP/SipService.swift` | Linphone register/call/video |
| `SIP/CallUI.swift` | Полноэкранный звонок + DTMF |
| `UI/Components.swift` | StatusDot |
| `UI/*` | Вкладки Свет / Домофон / Настройки |

## Permissions (Info.plist)

- Микрофон, камера
- Локальная сеть
- Background mode: `audio` (на время звонка)

CallKit / VoIP push — не в MVP.
