# Corridor panel — общий каталог

Источник правды: [`topics.json`](./topics.json).  
Описания топиков: [`topic_registry.md`](./topic_registry.md).

```bash
python3 scripts/gen_corridor_topics.py
```

Пишет:

- `macos/CorridorPanel/.../Topics.swift`
- `android/corridor-panel/.../Topics.kt`
- копии JSON в assets / Resources

Порог «молчит»: `staleAfterSec` (по умолчанию 120).

Камера: `preferRtsp` → субпоток `…/102`, при сбое — ISAPI JPEG.

**Улица:** Zigbee «Уличный датчик температуры» на наружном блоке кондея → блок под видео двери.

**Карниз:** виджет RGB+белый как на квадратных планшетах (room-panel).

**iOS HomePanel** — полный дом (кухня/комната/AC); corridor JSON для панелей коридора / macOS.

**Дожать в WB:** мастер `wb-mr6cu_43/K1` добавлен в каталог; если модуля нет на шине — в UI будет «молчит».
