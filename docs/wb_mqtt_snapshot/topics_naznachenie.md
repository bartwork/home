# MQTT-топики ↔ назначение по схеме щита

Источники: `shit_ABB_AT62_polnaya_tsepochka.html`, `shit_ABB_AT62_shema.html`, live MQTT `192.168.1.9`, `wb-mqtt-serial` config.

## Соответствие модулей схемы → MQTT device

| Схема | MQTT device | slave_id | Уверенность |
|---|---|---:|---|
| `MR6C#1` | `wb-mr6cv3_225` | 225 | средняя (эвристика): предположительно: все K=1 (ТП), в т.ч. FREE |
| `MR6C#2` | `wb-mr6cv3_227` | 227 | средняя (эвристика): предположительно: K1=0 остальные 1 |
| `MR6C#3` | `wb-mr6cv3_239` | 239 | средняя (эвристика): предположительно: все K=0 (сан. ночью) |
| `MDM3#1` | `wb-mdm3_218` | 218 | средняя (эвристика): предположительно: порядок в wb-mqtt-serial |
| `MDM3#2` | `wb-mdm3_75` | 75 | средняя (эвристика): предположительно: порядок в wb-mqtt-serial |
| `MDM3#3` | `wb-mdm3_76` | 76 | средняя (эвристика): предположительно: порядок в wb-mqtt-serial |
| `MR6CU#1` | `wb-mr6cu_43` | 43 | низкая (1 модуль на 2 роли): единственный CU на шине; канал сушилки уточнить |
| `MR6CU#2` | `wb-mr6cu_43` | 43 | низкая (1 модуль на 2 роли): единственный CU на шине; канал ESB уточнить |
| `WB-LED` | `wb-led_221` | 221 | высокая: уверенно: единственный WB-LED |

> **Важно:** в `wb-mqtt-serial` нет человекочитаемых имён — только `slave_id`. Маппинг MR6C/MDM3 помечен как предположительный; WB-LED = №35 уверенно. На шине один `WB-MR6CU` (43), хотя в схеме CU#1 сушилка и CU#2 ESB.

## Силовые каналы (из полной цепочки)

| Кабель | Назначение | Схема | УЗО / автомат | MQTT topic | control |
|---:|---|---|---|---|---|
| №1 | Тёплый пол коридор | `MR6C#1 ch1` | УЗО5 · B16 ТП К+Т | `/devices/wb-mr6cv3_225/controls/K1` | `K1` |
| №3 | Тёплый пол туалет | `MR6C#1 ch2` | УЗО5 · B16 ТП К+Т | `/devices/wb-mr6cv3_225/controls/K2` | `K2` |
| №4 | Тёплый пол ванна | `MR6C#1 ch4` | УЗО5 · B16 ТП В+Кух | `/devices/wb-mr6cv3_225/controls/K4` | `K4` |
| №5 | Сушилка | `CU#1→контактор` | УЗО3 · B16 сушилка | `/devices/wb-mr6cu_43/controls/K*` — канал уточнить | `K? (уточнить)` |
| №15 | Тёплый пол кухня | `MR6C#1 ch5` | УЗО5 · B16 ТП В+Кух | `/devices/wb-mr6cv3_225/controls/K5` | `K5` |
| №27 | Лента шкаф/кладовая (230В → локальный БП 24В у шкафа) | `MR6C#2 ch4` | УЗО4 · B10 кладовая | `/devices/wb-mr6cv3_227/controls/K4` | `K4` |
| №28 | Свет кладовая | `MR6C#2 ch5` | УЗО4 · B10 кладовая | `/devices/wb-mr6cv3_227/controls/K5` | `K5` |
| №29 | Бра левая кровать (диммер) | `MDM3#3 ch2` | УЗО4 · B10 бра+кор | `/devices/wb-mdm3_76/controls/Channel 2` | `Channel 2` |
| №30 | Основная люстра | `MDM3#2 ch1` | УЗО4 · B10 комн.дим | `/devices/wb-mdm3_75/controls/Channel 1` | `Channel 1` |
| №31 | Точки у люстры | `MDM3#2 ch2` | УЗО4 · B10 комн.дим | `/devices/wb-mdm3_75/controls/Channel 2` | `Channel 2` |
| №32 | Бра правая кровать (диммер) | `MDM3#3 ch3` | УЗО4 · B10 бра+кор | `/devices/wb-mdm3_76/controls/Channel 3` | `Channel 3` |
| №35 | Лента карниза (24В) | `WB-LED` | УЗО7 · D6→ARV | `/devices/wb-led_221/controls/RGB Strip` (+ Brightness/Hue/Sat, Channel 4) | `RGB Strip` |
| №37 | Свет лоджия | `MDM3#2 ch3` | УЗО4 · B10 комн.дим | `/devices/wb-mdm3_75/controls/Channel 3` | `Channel 3` |
| №48 | Бра кухня | `MR6C#2 ch1` | УЗО1 · B10 кух.реле | `/devices/wb-mr6cv3_227/controls/K1` | `K1` |
| №50 | Основной свет кухня (диммер) | `MDM3#1 ch1` | УЗО1 · B10 кух.дим | `/devices/wb-mdm3_218/controls/Channel 1` | `Channel 1` |
| №51 | Левый свет кухня | `MDM3#1 ch2` | УЗО1 · B10 кух.дим | `/devices/wb-mdm3_218/controls/Channel 2` | `Channel 2` |
| №52 | Правый свет кухня | `MDM3#1 ch3` | УЗО1 · B10 кух.дим | `/devices/wb-mdm3_218/controls/Channel 3` | `Channel 3` |
| №53 | Подсветка кухни | `MR6C#2 ch2` | УЗО1 · B10 кух.реле | `/devices/wb-mr6cv3_227/controls/K2` | `K2` |
| №56 | Вытяжка кухня | `MR6C#2 ch3` | УЗО1 · B10 кух.реле | `/devices/wb-mr6cv3_227/controls/K3` | `K3` |
| №61 | Свет ванна | `MR6C#3 ch1` | УЗО2 · B10 свет.сан | `/devices/wb-mr6cv3_239/controls/K1` | `K1` |
| №62 | Зеркало ванна | `MR6C#3 ch2` | УЗО2 · B10 свет.сан | `/devices/wb-mr6cv3_239/controls/K2` | `K2` |
| №64 | Вытяжка ванна | `MR6C#3 ch3` | УЗО2 · B10 свет.сан | `/devices/wb-mr6cv3_239/controls/K3` | `K3` |
| №68 | Свет коридор | `MDM3#3 ch1` | УЗО4 · B10 бра+кор | `/devices/wb-mdm3_76/controls/Channel 1` | `Channel 1` |
| №69 | Зеркало туалет | `MR6C#3 ch4` | УЗО2 · B10 свет.сан | `/devices/wb-mr6cv3_239/controls/K4` | `K4` |
| №71 | Свет туалет | `MR6C#3 ch5` | УЗО2 · B10 свет.сан | `/devices/wb-mr6cv3_239/controls/K5` | `K5` |
| №72 | Вытяжка туалет | `MR6C#3 ch6` | УЗО2 · B10 свет.сан | `/devices/wb-mr6cv3_239/controls/K6` | `K6` |

## Свободные / служебные каналы

| Схема | MQTT topic (предпол.) | Назначение |
|---|---|---|
| MR6C#1 ch3 / ch6 | `/devices/wb-mr6cv3_225/controls/K3`, `K6` | FREE по схеме |
| MR6C#2 ch6 | `/devices/wb-mr6cv3_227/controls/K6` | FREE по схеме |
| MR6CU#2 · катушка ESB | `/devices/wb-mr6cu_43/controls/K?` | Мастер «отъезд» / питание УЗО1–5 |
| MR6CU#1 · сушилка №5 | `/devices/wb-mr6cu_43/controls/K?` | Катушка контактора сушилки |
| DI MR6C (EH04 / дверь / мастер) | `/devices/.../controls/Input N` | Входы MR6C v3, не CU |
| wb-gpio EXT1_K1…K8 | `/devices/wb-gpio/controls/EXT1_K*` | Расширение контроллера (ISEO/SSR?) |

## Сводная таблица топик → назначение

| MQTT topic | Тип | Назначение (схема) | Кабель |
|---|---|---|---:|
| `/devices/wb-mr6cv3_225/controls/K1` | switch | Тёплый пол коридор | №1 |
| `/devices/wb-mr6cv3_225/controls/K2` | switch | Тёплый пол туалет | №3 |
| `/devices/wb-mr6cv3_225/controls/K4` | switch | Тёплый пол ванна | №4 |
| `/devices/wb-mr6cu_43/controls/K? ← уточнить` | switch | Сушилка | №5 |
| `/devices/wb-mr6cv3_225/controls/K5` | switch | Тёплый пол кухня | №15 |
| `/devices/wb-mr6cv3_227/controls/K4` | switch | Лента шкаф/кладовая (230В → локальный БП 24В у шкафа) | №27 |
| `/devices/wb-mr6cv3_227/controls/K5` | switch | Свет кладовая | №28 |
| `/devices/wb-mdm3_76/controls/Channel 2` | range | Бра левая кровать (диммер) | №29 |
| `/devices/wb-mdm3_75/controls/Channel 1` | range | Основная люстра | №30 |
| `/devices/wb-mdm3_75/controls/Channel 2` | range | Точки у люстры | №31 |
| `/devices/wb-mdm3_76/controls/Channel 3` | range | Бра правая кровать (диммер) | №32 |
| `/devices/wb-led_221/controls/RGB Strip` | switch | Лента карниза (вкл) | №35 |
| `/devices/wb-led_221/controls/RGB Strip Brightness` | range | Лента карниза (яркость) | №35 |
| `/devices/wb-led_221/controls/RGB Palette` | rgb | Лента карниза (цвет) | №35 |
| `/devices/wb-led_221/controls/Channel 4` | switch | Лента карниза · Channel 4 | №35 |
| `/devices/wb-mdm3_75/controls/Channel 3` | range | Свет лоджия | №37 |
| `/devices/wb-mr6cv3_227/controls/K1` | switch | Бра кухня | №48 |
| `/devices/wb-mdm3_218/controls/Channel 1` | range | Основной свет кухня (диммер) | №50 |
| `/devices/wb-mdm3_218/controls/Channel 2` | range | Левый свет кухня | №51 |
| `/devices/wb-mdm3_218/controls/Channel 3` | range | Правый свет кухня | №52 |
| `/devices/wb-mr6cv3_227/controls/K2` | switch | Подсветка кухни | №53 |
| `/devices/wb-mr6cv3_227/controls/K3` | switch | Вытяжка кухня | №56 |
| `/devices/wb-mr6cv3_239/controls/K1` | switch | Свет ванна | №61 |
| `/devices/wb-mr6cv3_239/controls/K2` | switch | Зеркало ванна | №62 |
| `/devices/wb-mr6cv3_239/controls/K3` | switch | Вытяжка ванна | №64 |
| `/devices/wb-mdm3_76/controls/Channel 1` | range | Свет коридор | №68 |
| `/devices/wb-mr6cv3_239/controls/K4` | switch | Зеркало туалет | №69 |
| `/devices/wb-mr6cv3_239/controls/K5` | switch | Свет туалет | №71 |
| `/devices/wb-mr6cv3_239/controls/K6` | switch | Вытяжка туалет | №72 |

## Реле K1–K3 на MDM3 (есть в MQTT, нет в силовой таблице кабелей)

| MQTT topic | Примечание |
|---|---|
| `/devices/wb-mdm3_218/controls/K1` | доп. реле модуля `MDM3#1` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_218/controls/K2` | доп. реле модуля `MDM3#1` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_218/controls/K3` | доп. реле модуля `MDM3#1` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_75/controls/K1` | доп. реле модуля `MDM3#2` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_75/controls/K2` | доп. реле модуля `MDM3#2` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_75/controls/K3` | доп. реле модуля `MDM3#2` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_76/controls/K1` | доп. реле модуля `MDM3#3` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_76/controls/K2` | доп. реле модуля `MDM3#3` — назначение не описано в `polnaya_tsepochka` |
| `/devices/wb-mdm3_76/controls/K3` | доп. реле модуля `MDM3#3` — назначение не описано в `polnaya_tsepochka` |
