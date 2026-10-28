# Сверка discovery ↔ MQTT snapshot

Снято с `tcp://192.168.1.2:1883` (исторический IP; сейчас `192.168.1.1`). Живую БД `data/home.db` открыть не удалось (файл не SQLite).

## Итог

- Control-топиков: **314** (устройств: 25)
- Hardware `wb-*`: **215**; системные: **99**
- Кандидаты для UI (реле/каналы/диммеры/RGB, без inputs/counters/serial): **67**
- `ParseTopic` / `ClassifyWBType`: все snapshot-топики парсятся; классификация совпадает с dump
- `wb-w1` в этом снимке нет (ранее мелькал, retained мог не прийти за окно)

## Классификация (как в discovery)

| suggested_type | all | wb-* |
|---|---:|---:|
| `range` | 15 | 13 |
| `raw` | 62 | 9 |
| `sensor` | 101 | 86 |
| `switch` | 136 | 107 |

## Замечания для discovery

1. **Шум в staging**: counters, Serial, Input N, Bus Power Supply, AC on L-N, Overcurrent, network/metrics/system — всё попадёт в «Обнаружено», если не фильтровать.
2. **`rgb` → `raw`**: `WB-LED RGB Palette` (`76;0;130`) не получит отдельный виджет; возможно стоит `rgb` type.
3. **Пробелы в control**: `Channel 1`, `EXT1_K1` ок — `ParseTopic` режет по `/`, имя control сохраняется.
4. **Inputs как switch**: входы модулей классифицируются как `switch` (корректно по WB meta), но для accept обычно не нужны.
5. **Range на MDM3**: Channel 1/2/3 = диммеры (сейчас 100); K1/K2/K3 = доп. реле.

## Полезные controls для accept

### `wb-gpio` (18)

| control | class | value | topic |
|---|---|---|---|
| 5V_OUT | `switch` | 0 | `/devices/wb-gpio/controls/5V_OUT` |
| A1_IN | `switch` | 0 | `/devices/wb-gpio/controls/A1_IN` |
| A1_OUT | `switch` | 0 | `/devices/wb-gpio/controls/A1_OUT` |
| A2_IN | `switch` | 0 | `/devices/wb-gpio/controls/A2_IN` |
| A2_OUT | `switch` | 0 | `/devices/wb-gpio/controls/A2_OUT` |
| A3_IN | `switch` | 0 | `/devices/wb-gpio/controls/A3_IN` |
| A3_OUT | `switch` | 0 | `/devices/wb-gpio/controls/A3_OUT` |
| D1_IN | `switch` | 0 | `/devices/wb-gpio/controls/D1_IN` |
| D1_OUT | `switch` | 0 | `/devices/wb-gpio/controls/D1_OUT` |
| EXT1_K1 | `switch` | 1 | `/devices/wb-gpio/controls/EXT1_K1` |
| EXT1_K2 | `switch` | 1 | `/devices/wb-gpio/controls/EXT1_K2` |
| EXT1_K3 | `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K3` |
| EXT1_K4 | `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K4` |
| EXT1_K5 | `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K5` |
| EXT1_K6 | `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K6` |
| EXT1_K7 | `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K7` |
| EXT1_K8 | `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K8` |
| V_OUT | `switch` | 1 | `/devices/wb-gpio/controls/V_OUT` |

### `wb-led_221` (7)

| control | class | value | topic |
|---|---|---|---|
| Channel 4 | `switch` | 0 | `/devices/wb-led_221/controls/Channel 4` |
| Channel 4 Brightness | `range` | 100 | `/devices/wb-led_221/controls/Channel 4 Brightness` |
| RGB Palette | `raw` | 76;0;130 | `/devices/wb-led_221/controls/RGB Palette` |
| RGB Strip | `switch` | 0 | `/devices/wb-led_221/controls/RGB Strip` |
| RGB Strip Brightness | `range` | 51 | `/devices/wb-led_221/controls/RGB Strip Brightness` |
| RGB Strip Hue | `range` | 275 | `/devices/wb-led_221/controls/RGB Strip Hue` |
| RGB Strip Saturation | `range` | 100 | `/devices/wb-led_221/controls/RGB Strip Saturation` |

### `wb-mdm3_218` (6)

| control | class | value | topic |
|---|---|---|---|
| Channel 1 | `range` | 100 | `/devices/wb-mdm3_218/controls/Channel 1` |
| Channel 2 | `range` | 100 | `/devices/wb-mdm3_218/controls/Channel 2` |
| Channel 3 | `range` | 100 | `/devices/wb-mdm3_218/controls/Channel 3` |
| K1 | `switch` | 0 | `/devices/wb-mdm3_218/controls/K1` |
| K2 | `switch` | 1 | `/devices/wb-mdm3_218/controls/K2` |
| K3 | `switch` | 1 | `/devices/wb-mdm3_218/controls/K3` |

### `wb-mdm3_75` (6)

| control | class | value | topic |
|---|---|---|---|
| Channel 1 | `range` | 100 | `/devices/wb-mdm3_75/controls/Channel 1` |
| Channel 2 | `range` | 100 | `/devices/wb-mdm3_75/controls/Channel 2` |
| Channel 3 | `range` | 100 | `/devices/wb-mdm3_75/controls/Channel 3` |
| K1 | `switch` | 0 | `/devices/wb-mdm3_75/controls/K1` |
| K2 | `switch` | 1 | `/devices/wb-mdm3_75/controls/K2` |
| K3 | `switch` | 1 | `/devices/wb-mdm3_75/controls/K3` |

### `wb-mdm3_76` (6)

| control | class | value | topic |
|---|---|---|---|
| Channel 1 | `range` | 100 | `/devices/wb-mdm3_76/controls/Channel 1` |
| Channel 2 | `range` | 100 | `/devices/wb-mdm3_76/controls/Channel 2` |
| Channel 3 | `range` | 100 | `/devices/wb-mdm3_76/controls/Channel 3` |
| K1 | `switch` | 1 | `/devices/wb-mdm3_76/controls/K1` |
| K2 | `switch` | 1 | `/devices/wb-mdm3_76/controls/K2` |
| K3 | `switch` | 1 | `/devices/wb-mdm3_76/controls/K3` |

### `wb-mr6cu_43` (6)

| control | class | value | topic |
|---|---|---|---|
| K1 | `switch` | 1 | `/devices/wb-mr6cu_43/controls/K1` |
| K2 | `switch` | 0 | `/devices/wb-mr6cu_43/controls/K2` |
| K3 | `switch` | 0 | `/devices/wb-mr6cu_43/controls/K3` |
| K4 | `switch` | 0 | `/devices/wb-mr6cu_43/controls/K4` |
| K5 | `switch` | 0 | `/devices/wb-mr6cu_43/controls/K5` |
| K6 | `switch` | 0 | `/devices/wb-mr6cu_43/controls/K6` |

### `wb-mr6cv3_225` (6)

| control | class | value | topic |
|---|---|---|---|
| K1 | `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K1` |
| K2 | `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K2` |
| K3 | `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K3` |
| K4 | `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K4` |
| K5 | `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K5` |
| K6 | `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K6` |

### `wb-mr6cv3_227` (6)

| control | class | value | topic |
|---|---|---|---|
| K1 | `switch` | 0 | `/devices/wb-mr6cv3_227/controls/K1` |
| K2 | `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K2` |
| K3 | `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K3` |
| K4 | `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K4` |
| K5 | `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K5` |
| K6 | `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K6` |

### `wb-mr6cv3_239` (6)

| control | class | value | topic |
|---|---|---|---|
| K1 | `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K1` |
| K2 | `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K2` |
| K3 | `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K3` |
| K4 | `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K4` |
| K5 | `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K5` |
| K6 | `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K6` |

## Файлы

- `wb_controls.tsv` / `wb_controls.json` — полный список
- `wb_modules.md` — разбор по модулям
- `discovery_compare.md` — этот отчёт
