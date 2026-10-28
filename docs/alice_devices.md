# Устройства Яндекс Алисы (Wiren Board)

Снято с `GET http://192.168.1.1/api/integrations/alice` (2026-10-23).  
Как править: [`alice_setup.md`](alice_setup.md). MQTT-источник: [`mqtt_topics.md`](mqtt_topics.md).

UI: http://192.168.1.1/#/integrations/alice

Всего устройств: **25** (19 свет + 4 тёплый пол + 2 кондиционер).

## Комнаты

| Комната | Alice room_id | Устройств |
|---|---|---:|
| Коридор | `225bdb64-261020145637-eaed216b-65ed-4e66-b76e-f390c80bb121` | 2 |
| Зал | `225bdb64-261020145706-164c8a50-75db-4c74-9fbc-bcf67699c3f3` | 7 |
| Лоджия | `225bdb64-261020145721-22a64093-54ff-4023-998c-36faa9b4aa1a` | 1 |
| Кухня | `225bdb64-261020145656-0f9dfa22-8123-491e-bc0c-109926f9e143` | 7 |
| Кладовка | `225bdb64-261020145728-d8bbe598-e18d-4499-aa9f-c41b45112401` | 2 |
| Ванна | `225bdb64-261020145650-00c3ccf9-3b44-4ea3-8084-1495ae06a93a` | 3 |
| Туалет | `225bdb64-261020145646-9d6eb404-1354-48fd-b1d1-db5845ffe30e` | 3 |

## По комнатам

### Коридор

| Имя | Тип | Умения / свойства (mqtt) |
|---|---|---|
| Основной свет | `light` | on_off `wb-mdm3_76/K1` · brightness `wb-mdm3_76/Channel 1` |
| Тёплый пол | `thermostat` | on_off `wb-mr6cv3_225/K1` · temp `wb-m1w2_83/External Sensor 1` |

ids:

- Основной свет — `225bdb64-261020173058-cbc0c555-0120-42ab-a163-b4e872941c7e`
- Тёплый пол — `225bdb64-261023095221-fd7c7a40-2921-4a6c-892d-32bd17076039`

### Зал

| Имя | Тип | Умения (mqtt) |
|---|---|---|
| Основной свет | `light` | on_off `wb-mdm3_75/K1` · brightness `wb-mdm3_75/Channel 1` |
| Точки у люстры | `light` | on_off `wb-mdm3_75/K2` · brightness `wb-mdm3_75/Channel 2` |
| Бра левое | `light` | on_off `wb-mdm3_76/K2` · brightness `wb-mdm3_76/Channel 2` |
| Бра правое | `light` | on_off `wb-mdm3_76/K3` · brightness `wb-mdm3_76/Channel 3` |
| Лента карниза | `light.strip` | on_off `wb-led_221/RGB Strip` · brightness `…/RGB Strip Brightness` · color rgb `…/RGB Palette` |
| Белый карниз | `light` | on_off `wb-led_221/Channel 4` · brightness `wb-led_221/Channel 4 Brightness` |
| Кондиционер | `thermostat.ac` | on_off · target · mode · fan · swing `ONOKOM-AIR-HR-1-MB-B_2` · indoor temp |

ids:

- Основной свет — `225bdb64-261020145920-c8cb4557-f470-4a02-b2c7-9c2e5f41059c`
- Точки у люстры — `225bdb64-261020173058-0c4e4616-8f1c-4b23-b28d-059cf5b4d544`
- Бра левое — `225bdb64-261020173058-1cb55e5f-915e-4d9b-b074-98b2f73c2f1e` (MQTT: бра около кладовки)
- Бра правое — `225bdb64-261020173058-d238fb08-41eb-40d7-9b4f-340101aee39c` (MQTT: бра около лоджии)
- Лента карниза — `225bdb64-261020173058-330ade27-c0f3-44ae-8015-30ee7ca186ba`
- Белый карниз — `225bdb64-261020173453-471d30c4-83d5-48bb-bcee-d6e492197205`
- Кондиционер — `225bdb64-261023095606-f2c30055-76c9-4b36-9458-387d395a5aee`

### Лоджия

| Имя | Тип | Умения (mqtt) |
|---|---|---|
| Свет | `light` | on_off `wb-mdm3_75/K3` · brightness `wb-mdm3_75/Channel 3` |

id: `225bdb64-261020173058-6418d3bf-902b-41b4-aef8-ada632c22bb6`

### Кухня

| Имя | Тип | Умения / свойства (mqtt) |
|---|---|---|
| Свет у окна | `light` | on_off `wb-mdm3_218/K1` · brightness `wb-mdm3_218/Channel 1` |
| Лампа | `light` | on_off `wb-mdm3_218/K2` · brightness `wb-mdm3_218/Channel 2` |
| Свет у двери | `light` | on_off `wb-mdm3_218/K3` · brightness `wb-mdm3_218/Channel 3` |
| Бра | `light.lamp` | on_off `wb-mr6cv3_239/K1` |
| Подсветка | `light.strip` | on_off `wb-mr6cv3_239/K2` |
| Тёплый пол | `thermostat` | on_off `wb-mr6cv3_225/K5` · temp `wb-m1w2_82/External Sensor 2` |
| Кондиционер | `thermostat.ac` | on_off · target · mode · fan · swing `ONOKOM-AIR-HR-1-MB-B_1` · indoor temp |

ids:

- Свет у окна — `225bdb64-261020173059-21965e72-e4ef-42fc-8eaf-ac5986b3be5b`
- Лампа — `225bdb64-261020173059-1279edad-aab4-4941-bb97-9a39d68d2d9e`
- Свет у двери — `225bdb64-261020173059-c389d14a-00aa-46c7-8e65-fcaff74ed52d`
- Бра — `225bdb64-261020173059-a6eb8cf7-d4d4-4561-aa5f-bb3f690dd8bf`
- Подсветка — `225bdb64-261020173059-1c3967f7-b3d5-46f8-8d4c-bbfe7870b397`
- Тёплый пол — `225bdb64-261023095222-dcfb8875-2a37-4336-af4f-1a75741eaefc`
- Кондиционер — `225bdb64-261023095606-a1fd2b92-946f-41f5-a7a1-ecfd8e61d074`

### Кладовка

| Имя | Тип | Умения (mqtt) |
|---|---|---|
| Лента | `light.strip` | on_off `wb-mr6cv3_239/K4` |
| Свет | `light` | on_off `wb-mr6cv3_239/K5` |

ids:

- Лента — `225bdb64-261020173059-5219ad56-9027-4be4-ba9f-3f7a18118164`
- Свет — `225bdb64-261020173059-1bc23d8d-4fb0-404d-a22e-7c61ca56c4cd`

### Ванна

| Имя | Тип | Умения / свойства (mqtt) |
|---|---|---|
| Основной свет | `light` | on_off `wb-mr6cv3_227/K1` |
| Зеркало | `light` | on_off `wb-mr6cv3_227/K2` |
| Тёплый пол | `thermostat` | on_off `wb-mr6cv3_225/K4` · temp `wb-m1w2_82/External Sensor 1` |

ids:

- Основной свет — `225bdb64-261020173100-3db52a39-b772-4591-917d-afedd2096266`
- Зеркало — `225bdb64-261020173100-c9980896-9440-414b-a3d8-42225845bc02`
- Тёплый пол — `225bdb64-261023095222-0489a3b1-07fc-4024-a68d-826153db1abb`

### Туалет

| Имя | Тип | Умения / свойства (mqtt) |
|---|---|---|
| Зеркало | `light` | on_off `wb-mr6cv3_227/K4` |
| Свет | `light` | on_off `wb-mr6cv3_227/K5` |
| Тёплый пол | `thermostat` | on_off `wb-mr6cv3_225/K2` · temp `wb-m1w2_83/External Sensor 2` |

ids:

- Зеркало — `225bdb64-261020173100-c6429e05-e89c-4989-a03a-71752edffffd`
- Свет — `225bdb64-261020173100-034ff2c5-826c-4c91-98cf-8b236d208445`
- Тёплый пол — `225bdb64-261023095222-0417a072-1e4b-424a-b0da-e5e9a5e411c2`

## Не в Алисе (пока)

Вытяжки, MSW «воздух», мастер-выключатель, Eco/Sleep кондея — только в нашем UI / MQTT.

## Кондиционеры ONOKOM (кратко)

Тип `devices.types.thermostat.ac`. Режимы мапятся через `mqtt_value_match` (числа ONOKOM ↔ значения Алисы).

| Комната | Шлюз | Умения |
|---|---|---|
| Кухня | `ONOKOM-AIR-HR-1-MB-B_1` | Active, Target temperature, Mode, Smart fan speed, Vanes swing + Indoor air temperature |
| Зал | `ONOKOM-AIR-HR-1-MB-B_2` | то же |

| Умение Алисы | MQTT | Маппинг |
|---|---|---|
| on_off | `Active` | 0/1 |
| range temperature | `Target temperature` | 16…30 °C |
| mode thermostat | `Mode` | heat=1 cool=2 auto=3 dry=4 fan_only=5 |
| mode fan_speed | `Smart fan speed` | auto=0 quiet=1 low=2 medium=3 high=4 turbo=5 |
| mode swing | `Vanes swing` | stationary=0 auto=1 horizontal=2 vertical=3 |
| float temperature | `Indoor air temperature` | °C (только чтение) |

Не заведено: Eco mode, Sleep mode, Horizontal/Vertical vanes (позиции) — в Алисе нет прямых аналогов / избыточно.

Фразы: «включи кондиционер в зале», «поставь 24 градуса», «режим охлаждения», «тихий вентилятор», «какая температура на кондиционере».

## Тёплые полы (кратко)

Тип `devices.types.thermostat`: вкл/выкл реле + свойство температуры (без уставки — у нас нет терморегулятора, только реле).

| Комната | Реле | Датчик |
|---|---|---|
| Коридор | `wb-mr6cv3_225/K1` | `wb-m1w2_83/External Sensor 1` |
| Туалет | `wb-mr6cv3_225/K2` | `wb-m1w2_83/External Sensor 2` |
| Ванна | `wb-mr6cv3_225/K4` | `wb-m1w2_82/External Sensor 1` |
| Кухня | `wb-mr6cv3_225/K5` | `wb-m1w2_82/External Sensor 2` |

Фразы: «включи тёплый пол в ванной», «какая температура тёплого пола на кухне?».

## WB-LED карниз (кратко)

| Устройство Алисы | Каналы модуля |
|---|---|
| Лента карниза | RGB Strip + Brightness + RGB Palette |
| Белый карниз | Channel 4 + Channel 4 Brightness |

Два устройства намеренно: Алиса лучше управляет RGB и белым порознь («включи красный на ленте» / «включи белый карниз»).
