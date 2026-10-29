# HomePanel (iOS)

SwiftUI-приложение: MQTT-свет (Wiren Board) + SIP-видеодомофон (Linphone → Asterisk).

Полная инструкция: [`docs/ios_homepanel.md`](../../docs/ios_homepanel.md)

## Быстрый старт

```bash
cd ios/HomePanel
xcodegen generate   # если меняли project.yml
open HomePanel.xcodeproj
```

Укажите Development Team в Signing, соберите на iPhone в LAN.

Дефолты: MQTT `192.168.1.1:1883`, SIP `102@192.168.1.9` / `soft102test`.
