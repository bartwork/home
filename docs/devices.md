# Устройства (топик → UI)

Excel: [`devices.xlsx`](devices.xlsx) — рабочая копия (дополнять там).

Типы: `switch` | `range` | `sensor` | `climate` | `led` | `raw`.  
Полный список топиков (вкл. siblings LED/climate): [`mqtt_topics.md`](mqtt_topics.md).  
Диммеры (`range`): яркость = `mqtt_topic`, вкл/выкл = `power_topic` (MDM3: Channel N → KN).  
Проверено: `да` (зелёный) · `да?` (жёлтый — пара могла быть перепутана) · пусто (не проверено).

| Проверено | Кабель | Имя | Комната | Тип | MQTT topic | Примечание |
|---|---:|---|---|---|---|---|
| да | 68 | основной свет | коридор | range | `/devices/wb-mdm3_76/controls/Channel 1` | |
| да | | мастер-выключатель | | switch | `/devices/wb-mr6cu_43/controls/K1` | |
| да | 30 | основной свет | комната | range | `/devices/wb-mdm3_75/controls/Channel 1` | люстра |
| да | 37 | лоджия свет | комната | range | `/devices/wb-mdm3_75/controls/Channel 3` | |
| да? | 27 | лента/свет | кладовка | switch | `/devices/wb-mr6cv3_239/controls/K4` | возм. ↔ №28 |
| да | 29 | бра около кладовки | комната | range | `/devices/wb-mdm3_76/controls/Channel 2` | |
| да | 32 | бра около лоджии | комната | range | `/devices/wb-mdm3_76/controls/Channel 3` | |
| да? | 28 | лента/свет | кладовка | switch | `/devices/wb-mr6cv3_239/controls/K5` | возм. ↔ №27 |
| да | 61 | ванна основной | ванна | switch | `/devices/wb-mr6cv3_227/controls/K1` | |
| да | 62 | ванна зеркало | ванна | switch | `/devices/wb-mr6cv3_227/controls/K2` | |
| да | 64 | ванна вытяжка | ванна | switch | `/devices/wb-mr6cv3_227/controls/K3` | |
| да | 69 | туалет зеркало | туалет | switch | `/devices/wb-mr6cv3_227/controls/K4` | |
| да? | 71 | туалет свет | туалет | switch | `/devices/wb-mr6cv3_227/controls/K5` | возм. ↔ №72 |
| да? | 72 | туалет вытяжка | туалет | switch | `/devices/wb-mr6cv3_227/controls/K6` | возм. ↔ №71 |
| | 1 | тёплый пол | коридор | switch | `/devices/wb-mr6cv3_225/controls/K1` | |
| | 3 | тёплый пол | туалет | switch | `/devices/wb-mr6cv3_225/controls/K2` | |
| | 4 | тёплый пол | ванна | switch | `/devices/wb-mr6cv3_225/controls/K4` | |
| | 5 | сушилка | | switch | `/devices/wb-mr6cu_43/controls/K?` | не K1 (мастер) |
| | 15 | тёплый пол | кухня | switch | `/devices/wb-mr6cv3_225/controls/K5` | |
| | 31 | точки у люстры | комната | range | `/devices/wb-mdm3_75/controls/Channel 2` | |
| | 35 | лента карниза | комната | led | `/devices/wb-led_221/controls/RGB Strip` | Brightness / Hue / Sat / Channel 4 |
| | 50 | основной свет | кухня | range | `/devices/wb-mdm3_218/controls/Channel 1` | |
| | 51 | левый свет | кухня | range | `/devices/wb-mdm3_218/controls/Channel 2` | |
| | 52 | правый свет | кухня | range | `/devices/wb-mdm3_218/controls/Channel 3` | |

## Заметки

- Актуальная проверка: ваша таблица → модули **кладовка = `239`**, **ванна/туалет = `227`**.
- Жёлтые: №27↔№28 (лента↔свет кладовки), №71↔№72 (свет↔вытяжка туалета).
