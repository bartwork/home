# Реестр MQTT-топиков — «Мой дом» (corridor)

Источник правды: [`topics.json`](./topics.json).  
Генерация кода: `python3 scripts/gen_corridor_topics.py` → `Topics.swift` / `Topics.kt`.  
Подписи в UI статистики: `TopicLabels` (macOS / Android).

Команды пишутся в `…/on` (retained=false), состояние читается из state-топика.

---

## Брокер

| Параметр | Значение |
|----------|----------|
| host | `192.168.1.1` |
| port | `1883` |
| staleAfterSec | `120` — нет MQTT дольше → «молчит» |

---

## Свет — диммеры (`dimmers`)

Канал яркости 0…100 + реле питания. Group = зона UI.

| id | Зона | Название | Channel | Power | Описание |
|----|------|----------|---------|-------|----------|
| hall_main | hall | Основной | `wb-mdm3_76` Channel 1 | K1 | Основной свет прихожей |
| room_main | room | Свет | `wb-mdm3_75` Channel 1 | K1 | Основной свет зала |
| room_extra | room | Точки | `wb-mdm3_75` Channel 2 | K2 | Точечные в зале |
| loggia | room | Лоджия | `wb-mdm3_75` Channel 3 | K3 | Лоджия |
| bra_left | room | Бра левое | `wb-mdm3_76` Channel 2 | K2 | Бра зала, левое |
| bra_right | room | Бра правое | `wb-mdm3_76` Channel 3 | K3 | Бра зала, правое |
| kitchen_window | kitchen | У окна | `wb-mdm3_218` Channel 1 | K1 | Кухня у окна |
| kitchen_lamp | kitchen | Лампа | `wb-mdm3_218` Channel 2 | K2 | Подвесной / лампа |
| kitchen_door | kitchen | У двери | `wb-mdm3_218` Channel 3 | K3 | Кухня у двери |

Полный топик: `/devices/<device>/controls/<control>`.

---

## Свет — выключатели (`switches`)

| id | Зона | Название | Топик | Описание |
|----|------|----------|-------|----------|
| master | master | Мастер-выкл. | `wb-mr6cu_43/K1` | Общий мастер дома |
| kitchen_bra | kitchen | Бра | `wb-mr6cv3_239/K1` | Бра кухни |
| kitchen_under | kitchen | Подсветка | `wb-mr6cv3_239/K2` | Подсветка кухни |
| kitchen_hood | kitchen | Вытяжка | `wb-mr6cv3_239/K3` | Вытяжка кухни |
| kladovka_strip | storage | Лента | `wb-mr6cv3_239/K4` | Лента кладовки |
| kladovka_light | storage | Свет | `wb-mr6cv3_239/K5` | Свет кладовки |
| bath_main | bath | Свет | `wb-mr6cv3_227/K1` | Свет ванной |
| bath_mirror | bath | Зеркало | `wb-mr6cv3_227/K2` | Зеркало ванной |
| bath_fan | bath | Вытяжка | `wb-mr6cv3_227/K3` | Вытяжка ванной |
| toilet_light | toilet | Свет | `wb-mr6cv3_227/K5` | Свет туалета |
| toilet_mirror | toilet | Зеркало | `wb-mr6cv3_227/K4` | Зеркало туалета |
| toilet_fan | toilet | Вытяжка | `wb-mr6cv3_227/K6` | Вытяжка туалета |

---

## Карниз LED (`led`) — `wb-led_221`

RGB-лента + отдельный белый Channel 4. Виджет как на room-panel.

| Ключ | Топик control | Описание |
|------|---------------|----------|
| strip | `RGB Strip` | Вкл/выкл RGB (0/1) |
| brightness | `RGB Strip Brightness` | Яркость RGB 0…100 |
| hue | `RGB Strip Hue` | Оттенок 0…360 |
| saturation | `RGB Strip Saturation` | Насыщенность 0…100 |
| hueChanging | `Hue Changing` | Автоцикл цвета |
| white | `Channel 4` | Белый канал вкл/выкл |
| whiteBrightness | `Channel 4 Brightness` | Яркость белого 0…100 |

---

## Воздух внутри (`air`) — `wb-msw-v3_108`

Мультисенсор в квартире (не улица).

| Ключ | Control | Описание |
|------|---------|----------|
| temperature | Temperature | °C внутри |
| humidity | Humidity | % |
| co2 | CO2 | ppm |
| voc | Air Quality (VOC) | VOC |
| illuminance | Illuminance | люкс |
| sound | Sound Level | уровень шума |
| motion | Current Motion | текущее движение |
| maxMotion | Max Motion | пик движения |

---

## Погода на улице (`weather`)

Zigbee-устройство **«Уличный датчик температуры»** — закреплён на наружном блоке кондиционера.

| Ключ | Топик | Описание |
|------|-------|----------|
| temperature | `/devices/Уличный датчик температуры/controls/temperature` | °C на улице |
| humidity | `…/humidity` | влажность % |
| battery | `…/battery` | заряд батареи |
| linkquality | `…/linkquality` | качество связи Zigbee |

UI: блок «Улица» под видео двери (macOS + Android).

---

## Кондиционеры (`climates`)

База + control. `_2` — зал, `_1` — кухня (ONOKOM-AIR-HR-1-MB-B).

| id | Название | base |
|----|----------|------|
| ac_room | Зал | `/devices/ONOKOM-AIR-HR-1-MB-B_2/controls` |
| ac_kitchen | Кухня | `/devices/ONOKOM-AIR-HR-1-MB-B_1/controls` |

Controls: `Active`, `Mode`, `Target temperature`, `Indoor air temperature`, `Smart fan speed`, `Vanes swing`, `Horizontal vanes`, `Vertical vanes`, `Eco mode`, `Sleep mode`.

---

## Тёплый пол (`floors`)

Реле на `wb-mr6cv3_225` + датчик `wb-m1w2_*`. Уставка/режим — виртуальные `/devices/home/controls/floor_<id>_setpoint|mode`.

| id | Название | deviceId | Relay | Sensor |
|----|----------|----------|-------|--------|
| floor_hall | Прихожая | 15 | K1 | m1w2_83 Ext 1 |
| floor_bath | Ванна | 17 | K4 | m1w2_82 Ext 1 |
| floor_toilet | Туалет | 16 | K2 | m1w2_83 Ext 2 |
| floor_kitchen | Кухня | 18 | K5 | m1w2_82 Ext 2 |

---

## Камера двери (`camera`)

Не MQTT. Hikvision KV6103: RTSP `…/102` (субпоток), fallback ISAPI JPEG. Рамка UI 4:3.

---

## Примечания

- Центр управления всем домом. Панели kitchen/room в розетках — локальное удобство, их топики не дублировать сюда без нужды.
- Мастер `wb-mr6cu_43/K1` — group=`master`.
- При добавлении топика: правка `topics.json` → gen → обновить этот реестр и `TopicLabels`.
