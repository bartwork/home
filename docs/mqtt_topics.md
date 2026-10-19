# MQTT-топики (каталог UI)

Источник правды для seed и экрана «Дом».  
Команды пишутся в тот же топик (Wiren Board: publish в `/devices/.../controls/...`, обычно с `/on` — bridge сам ходит в broker).

Типы UI: `switch` · `range` · `sensor` · `climate` · `led` · `floor` · `raw`.

---

## Каталог устройств

| Комната | Имя | Тип | Якорный топик | Power / siblings |
|---|---|---|---|---|
| коридор | основной свет | range | `/devices/wb-mdm3_76/controls/Channel 1` | power → `/devices/wb-mdm3_76/controls/K1` |
| — | мастер-выключатель | switch | `/devices/wb-mr6cu_43/controls/K1` | |
| комната | основной свет | range | `/devices/wb-mdm3_75/controls/Channel 1` | power → `…/K1` |
| комната | лоджия свет | range | `/devices/wb-mdm3_75/controls/Channel 3` | power → `…/K3` |
| комната | дополнительный свет | range | `/devices/wb-mdm3_75/controls/Channel 2` | power → `…/K2` |
| комната | бра около кладовки | range | `/devices/wb-mdm3_76/controls/Channel 2` | power → `…/K2` |
| комната | бра около лоджии | range | `/devices/wb-mdm3_76/controls/Channel 3` | power → `…/K3` |
| комната | лента карниза | **led** | `/devices/wb-led_221/controls/RGB Strip` | см. LED ниже |
| комната | кондиционер | climate | `/devices/ONOKOM-AIR-HR-1-MB-B_2/controls/Active` | см. climate ниже |
| кухня | свет у окна | range | `/devices/wb-mdm3_218/controls/Channel 1` | power → `…/K1` |
| кухня | лампа | range | `/devices/wb-mdm3_218/controls/Channel 2` | power → `…/K2` |
| кухня | свет у двери | range | `/devices/wb-mdm3_218/controls/Channel 3` | power → `…/K3` |
| кухня | бра | switch | `/devices/wb-mr6cv3_239/controls/K1` | |
| кухня | подсветка кухни | switch | `/devices/wb-mr6cv3_239/controls/K2` | |
| кухня | вытяжка | switch | `/devices/wb-mr6cv3_239/controls/K3` | |
| кухня | кондиционер | climate | `/devices/ONOKOM-AIR-HR-1-MB-B_1/controls/Active` | см. climate ниже |
| кухня | тёплый пол | **floor** | `/devices/wb-mr6cv3_225/controls/K5` | temp → `wb-m1w2_82` Ext.2 |
| кладовка | лента | switch | `/devices/wb-mr6cv3_239/controls/K4` | |
| кладовка | свет | switch | `/devices/wb-mr6cv3_239/controls/K5` | |
| ванна | основной свет | switch | `/devices/wb-mr6cv3_227/controls/K1` | |
| ванна | зеркало | switch | `/devices/wb-mr6cv3_227/controls/K2` | |
| ванна | вытяжка | switch | `/devices/wb-mr6cv3_227/controls/K3` | |
| ванна | тёплый пол | **floor** | `/devices/wb-mr6cv3_225/controls/K4` | temp → `wb-m1w2_82` Ext.1 |
| туалет | зеркало | switch | `/devices/wb-mr6cv3_227/controls/K4` | |
| туалет | свет | switch | `/devices/wb-mr6cv3_227/controls/K5` | |
| туалет | вытяжка | switch | `/devices/wb-mr6cv3_227/controls/K6` | |
| туалет | тёплый пол | **floor** | `/devices/wb-mr6cv3_225/controls/K2` | temp → `wb-m1w2_83` Ext.2 |
| коридор | тёплый пол | **floor** | `/devices/wb-mr6cv3_225/controls/K1` | temp → `wb-m1w2_83` Ext.1 |

Правило MDM3: `…/Channel N` → power `…/KN`.

---

## LED — `wb-led_221` (RGB Strip)

Якорь устройства в UI: **`/devices/wb-led_221/controls/RGB Strip`** (`0`/`1`).

| Control | Топик | Тип | Диапазон |
|---|---|---|---|
| RGB Strip | `/devices/wb-led_221/controls/RGB Strip` | switch | 0 / 1 |
| RGB Strip Brightness | `/devices/wb-led_221/controls/RGB Strip Brightness` | range | 0…100 (V) |
| RGB Strip Hue | `/devices/wb-led_221/controls/RGB Strip Hue` | range | 0…360 (H) |
| RGB Strip Saturation | `/devices/wb-led_221/controls/RGB Strip Saturation` | range | 0…100 (S) |
| Hue Changing | `/devices/wb-led_221/controls/Hue Changing` | switch | авто-смена оттенка; кнопка ↻ в виджете |
| Hue Changing Rate | `/devices/wb-led_221/controls/Hue Changing Rate` | sensor | мс на 1° (не UI) |
| RGB Palette | `/devices/wb-led_221/controls/RGB Palette` | rgb | `R;G;B` 0…255 (не якорь UI) |
| Channel 4 | `/devices/wb-led_221/controls/Channel 4` | switch | белый канал 0 / 1 |
| Channel 4 Brightness | `/devices/wb-led_221/controls/Channel 4 Brightness` | range | 0…100 |

UI: ручная смена H/S/V / избранное гасит `Hue Changing`.  
Избранные (API `/api/v1/color-favorites`, max 8; `POST …/reset` — записать стандарт в БД).  
Пока список пуст, UI показывает стандартную палитру без записи в БД; «+» сохраняет.

---

## Climate — ONOKOM AIR

Якорь: **`…/controls/Active`**.

Шлюзы:
- кухня: `/devices/ONOKOM-AIR-HR-1-MB-B_1/controls/Active`
- комната: `/devices/ONOKOM-AIR-HR-1-MB-B_2/controls/Active`

Sibling-controls (подставить тот же `device_id`):

| Control | Суффикс топика |
|---|---|
| Active | `/controls/Active` |
| Mode | `/controls/Mode` |
| Target temperature | `/controls/Target temperature` |
| Indoor air temperature | `/controls/Indoor air temperature` |
| Smart fan speed | `/controls/Smart fan speed` |
| Vanes swing | `/controls/Vanes swing` |
| Horizontal vanes | `/controls/Horizontal vanes` |
| Vertical vanes | `/controls/Vertical vanes` |
| Eco mode | `/controls/Eco mode` |
| Sleep mode | `/controls/Sleep mode` |

Пример (кухня, режим):  
`/devices/ONOKOM-AIR-HR-1-MB-B_1/controls/Mode`

---

## Floor — тёплый пол (`wb-mr6cv3_225` + `wb-m1w2_*`)

Якорь в UI: **реле** (`mqtt_topic`). Температура: **`power_topic`** → датчик `External Sensor N`.

| Комната | Реле | Датчик |
|---|---|---|
| коридор | `/devices/wb-mr6cv3_225/controls/K1` | `/devices/wb-m1w2_83/controls/External Sensor 1` |
| туалет | `/devices/wb-mr6cv3_225/controls/K2` | `/devices/wb-m1w2_83/controls/External Sensor 2` |
| ванна | `/devices/wb-mr6cv3_225/controls/K4` | `/devices/wb-m1w2_82/controls/External Sensor 1` |
| кухня | `/devices/wb-mr6cv3_225/controls/K5` | `/devices/wb-m1w2_82/controls/External Sensor 2` |

Датчики в discovery → `raw` (не принимать отдельно).

---

## Модули Wiren Board (кратко)

| ID | Модуль | Назначение |
|---|---|---|
| `wb-mdm3_75` | MDM3 | комната (люстра / точки / лоджия) |
| `wb-mdm3_76` | MDM3 | коридор + бра |
| `wb-mdm3_218` | MDM3 | кухня свет |
| `wb-mr6cv3_225` | реле | тёплые полы |
| `wb-mr6cv3_227` | реле | ванна / туалет |
| `wb-mr6cv3_239` | реле | кухня / кладовка |
| `wb-mr6cu_43` | реле | мастер |
| `wb-m1w2_82` | 1-Wire | датчики пола ванна / кухня |
| `wb-m1w2_83` | 1-Wire | датчики пола коридор / туалет |
| `wb-led_221` | WB-LED | лента карниза RGB |
| `ONOKOM-AIR-HR-1-MB-B_1` | шлюз AC | кондиционер кухня |
| `ONOKOM-AIR-HR-1-MB-B_2` | шлюз AC | кондиционер комната |

См. также: [`devices.md`](devices.md), [`wb_mqtt_snapshot/`](wb_mqtt_snapshot/).
