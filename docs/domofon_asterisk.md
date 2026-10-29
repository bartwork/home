# Домофон SIP + Asterisk (локальный тест)

Проверено **2026-10-29**: кнопка → видеозвонок в Linphone (Mac), аудио и видео работают.

Конфиг: [`scripts/asterisk-door/`](../scripts/asterisk-door/).  
Реестр сетевых устройств: [`network_devices.md`](network_devices.md).

## Схема

```
Hikvision DS-KV6103-PE1(C) ──SIP/RTP──► Asterisk (Docker на Mac)
   192.168.1.253                         192.168.1.9:5060
   SIP ext 100                                 │
                                               ▼
                                         Linphone (ext 101)
```

Кнопка домофона звонит на **Room No. 1** → маппинг **SIP Number `1`** → dialplan `from-door` → `Dial(PJSIP/101&PJSIP/102)`.

## Оборудование

| Роль | Модель | IP | MAC | SIP |
|------|--------|-----|-----|-----|
| Уличная панель | Hikvision **DS-KV6103-PE1(C)** | `192.168.1.253` | `bc:9b:5e:24:8a:54` | `100` |
| PBX | Asterisk 20 (`andrius/asterisk:20`) | `192.168.1.9` | Mac Wi‑Fi `60:3e:5f:49:6e:21` | — |
| Softphone | Linphone (macOS) | DHCP / тот же Mac | — | `101` |

- Имя на панели: `Door`
- S/N: `DS-KV6103-PE1(C)0120220913RRK56388257`
- Firmware: `V2.2.55 build 220816`
- Web UI: `http://192.168.1.253/` (логин `admin`, пароль — на устройстве)
- RTSP: порт `554`, main `1920×1080` H.264 + audio G.711u
- Реле замка: 1; alarm in: 4; PoE / Ethernet

В щитовой схеме раньше фигурировал **Beward B102S** — фактическая панель сейчас **Hikvision KV6103**.

## SIP-аккаунты Asterisk

| Ext | Роль | Password | Context |
|-----|------|----------|---------|
| `100` | Домофон | `door100test` | `from-door` |
| `101` | Softphone (ответ) | `soft101test` | `from-internal` |
| `102` | Softphone 2 / панель | `soft102test` | `from-internal` |

Сервер: `192.168.1.9`, порт **5060 UDP**, кодеки: `ulaw,alaw,g722` + видео `h264`.  
RTP: `10000–10050/udp`.

### Linphone (Mac) — поля

| Поле | Значение |
|------|----------|
| Username | `101` |
| Password | `soft101test` |
| Domain | `192.168.1.9` |
| Transport | **UDP** (не TLS) |
| Display name | любое |
| Registrar / Outbound proxy | пусто |

## Настройки Hikvision (важное)

1. **Network → SIP**
   - Enable VOIP Gateway: on
   - Proxy: `192.168.1.9`
   - Port: `5060`
   - Username / Auth ID: `100`
   - Password: `door100test`
   - Expires: `60`
   - Статус должен быть **Registered**

2. **Intercom → Press Button to Call**
   - Кнопка №1 → call number `1` (комната / внутренний номер)

3. **Intercom → Number Settings**
   - Room No. `1` → SIP Number `1`  
   - Без этой строки кнопка часто звонит только по протоколу Hikvision (indoor station), а не по SIP.

ISAPI (пример):

```bash
curl -u 'admin:PASSWORD' --digest \
  http://192.168.1.253/ISAPI/System/Network/SIP
```

Бэкап старого SIP (Novofon): [`scripts/asterisk-door/hik_sip_backup.xml`](../scripts/asterisk-door/hik_sip_backup.xml).

## Запуск Asterisk

```bash
cd scripts/asterisk-door
docker compose up -d

docker exec asterisk-door asterisk -rx 'pjsip show contacts'
docker exec asterisk-door asterisk -rx 'core show channels'
docker compose down   # остановить
```

Файлы:

- `docker-compose.yml` — порты 5060, 10000–10050
- `conf/pjsip.conf` — endpoints 100/101/102, `external_*_address=192.168.1.9`
- `conf/extensions.conf` — dialplan
- `conf/rtp.conf` — диапазон RTP

### Dialplan (кратко)

- `from-door`: номер `1` (и catch-all) → звонок на `101` и `102`
- `from-internal`: `100` → домофон (можно позвонить на панель из Linphone)

Для диагностики без softphone раньше использовали `Answer()` + `Milliwatt()` (тон ~1 кГц на динамике панели).

## Что проверено

- [x] Регистрация домофона на Asterisk
- [x] Нажатие кнопки → INVITE на Asterisk
- [x] Тестовый тон (Milliwatt) слышен на панели
- [x] Звонок в Linphone: **аудио + видео**

## Дальше (идеи)

- Softphone на Android kitchen/room panel (`101` / `102`)
- Home Assistant / SIP-HASS вместо Linphone на дашборде
- Постоянный Asterisk на сервере в LAN (не Docker Desktop на Mac) — стабильнее NAT/RTP
- Открытие двери: ISAPI remote open / DTMF → скрипт
- Вернуть Novofon: `PUT` бэкапа `hik_sip_backup.xml` в `/ISAPI/System/Network/SIP`

## Production: Asterisk на Wiren Board + запись

### Куда что ставить

| Задача | Куда | Почему |
|--------|------|--------|
| SIP-маршрутизация (звонки) | **WB7 Docker** или отдельный мини-ПК | Лёгкая нагрузка CPU/RAM |
| Постоянная запись видео+аудио с камеры | **Не на WB** — NUC / NAS / RPi+SSD + **Frigate** (или go2rtc+record) | 1080p 24/7 жрёт диск, CPU, IOPS; eMMC WB быстро убьёте |
| Запись разговора (аудио звонка) | Asterisk `MixMonitor` на WB или на том же хосте PBX | Аудио мало весит |
| Клип «кнопка нажата» (видео+аудио) | Frigate event / скрипт RTSP по webhook | Надёжнее, чем видео из Asterisk |

**Рекомендуемая схема:**

```
Домофон RTSP ──────────────────────────► Frigate (постоянная запись + детекции)
     │                                         │
     │ SIP                                     │ event «звонок» / MQTT
     ▼                                         ▼
 Asterisk (Docker на WB) ── MixMonitor ──► /mnt/data/asterisk/monitor/*.wav
     │
     ▼
 Softphone / панели
```

На WB: только Asterisk (+ опционально лёгкий webhook).  
Запись картинки: отдельная машина с диском.

### Asterisk на WB7 (Docker)

На контроллере (SSH):

```bash
apt update && apt install docker-ce
docker run --rm hello-world
```

Данные Docker у пакета WB лежат на `/mnt/data` — так и нужно.

Перенести конфиг с Mac:

```bash
# на Mac
scp -r scripts/asterisk-door root@192.168.1.1:/mnt/data/asterisk-door

# на WB — поправить IP в pjsip.conf:
#   external_media_address / external_signaling_address = 192.168.1.1
# на домофоне SIP proxy → 192.168.1.1

cd /mnt/data/asterisk-door
# network_mode: host удобнее на Linux (нет Docker Desktop NAT)
docker compose up -d
```

Образ: `andrius/asterisk:20` (есть **arm64**). На классическом WB7 чаще **armhf** — тогда нужен образ с `linux/arm/v7` или Asterisk из apt/`asterisk` пакета Debian, либо лёгкий хост arm64 (WB8 / NUC).

На WB предпочтительно `network_mode: host` + узкий RTP-диапазон, без проброса тысяч портов.

Ресурсы: Asterisk без транскодинга видео — обычно **<100–200 MB RAM**. Не гоняйте на WB Frigate/ffmpeg 1080p.

### Запись аудио звонка (Asterisk)

В dialplan:

```
exten => 1,1,NoOp(Door call)
 same => n,Set(MONITOR_FILENAME=/var/spool/asterisk/monitor/${STRFTIME(${EPOCH},,%Y%m%d-%H%M%S)}-door-${CALLERID(num)})
 same => n,MixMonitor(${MONITOR_FILENAME}.wav)
 same => n,Dial(PJSIP/101&PJSIP/102,45)
 same => n,StopMixMonitor()
 same => n,Hangup()
```

Том Docker: `/mnt/data/asterisk/monitor` → `/var/spool/asterisk/monitor`.

Это **только звук разговора** (оба направления). Видео SIP Asterisk нормально не пишет (нет нормального MixMonitor для H.264 как у аудио; транскодинга видео нет).

### Постоянная запись видео + аудио с камеры

Источник: **RTSP** панели (порт 554), не SIP.

Варианты:

1. **Frigate** (лучше для умного дома) — continuous + события, MQTT в WB/HA  
2. **go2rtc** + запись сегментов  
3. NVR (Synology / Hikvision)  

Пример URL (уточнить в UI / ISAPI):

```text
rtsp://admin:PASSWORD@192.168.1.253:554/Streaming/Channels/101
rtsp://admin:PASSWORD@192.168.1.253:554/Streaming/Channels/102   # субпоток
```

Хранить на HDD/SSD, не на eMMC WB. Ротация: 3–14 суток.

### Запись «при звонке» (видеоклип)

Не из SIP-видео, а так:

1. Кнопка → SIP INVITE на Asterisk **и/или** HTTP webhook / ISAPI event  
2. Триггер → Frigate `record` / `clips` на N секунд до/после **или** `ffmpeg` снял 30–60 с с RTSP в `/recordings/calls/`  
3. Параллельно Asterisk пишет `.wav` разговора  

Связка по времени: имя файла `20261029-224151-door` + клип Frigate с тем же timestamp.

Webhook с домофона (Action URL) или из dialplan:

```
same => n,System(curl -s http://frigate:5000/... )  ; или MQTT publish
```

или wb-rules: подписаться на событие / MQTT от Asterisk ARI.

### Чего не делать

- Писать 1080p 24/7 на eMMC WB  
- Ждать от Asterisk «как NVR» с видеозвонками  
- Транскодить H.264 на WB  

### Итоговая рекомендация для этого дома

1. **Asterisk** → Docker на **WB** (`192.168.1.1`), host network, MixMonitor на `/mnt/data`  
2. **Frigate** (или NVR) → отдельный хост с диском, RTSP с `192.168.1.253` постоянно  
3. При звонке: MixMonitor (аудио) + Frigate event/клип (видео)  
4. Softphone на панелях / Linphone без облака  

## Заметки

- Docker Desktop на Mac показывает contact как `192.168.65.1:…` — это NAT Docker; в нашем тесте RTP/видео всё равно прошли.
- Смена IP Mac (`192.168.1.9`) → править `external_media_address` / `external_signaling_address` в `pjsip.conf` и SIP proxy на домофоне.
- Пароли SIP тестовые, только для LAN; web-пароль панели в git не класть.
