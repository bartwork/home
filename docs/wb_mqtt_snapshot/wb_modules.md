# Wirenboard MQTT controls

Broker (снимок): `tcp://192.168.1.2:1883` — актуально: `192.168.1.1` (см. [`mqtt_topics.md`](../mqtt_topics.md)).

Controls: **314**

## Classification (discovery)

| suggested_type | count |
|---|---:|
| range | 15 |
| raw | 62 |
| sensor | 101 |
| switch | 136 |

Without meta type: **0**

## WB meta types

| type | count |
|---|---:|
| pushbutton | 6 |
| range | 15 |
| rgb | 1 |
| switch | 130 |
| temperature | 2 |
| text | 61 |
| value | 94 |
| voltage | 5 |

## Devices

### `alarms` — Alarms (1)

| control | type → class | value | topic |
|---|---|---|---|
| log | `text` → `raw` |  | `/devices/alarms/controls/log` |

### `buzzer` — Зуммер (3)

| control | type → class | value | topic |
|---|---|---|---|
| enabled | `switch` → `switch` | 0 | `/devices/buzzer/controls/enabled` |
| frequency | `range` → `range` | 4566 | `/devices/buzzer/controls/frequency` |
| volume | `range` → `range` | 100 | `/devices/buzzer/controls/volume` |

### `hwmon` — HW монитор (2)

| control | type → class | value | topic |
|---|---|---|---|
| Board Temperature | `temperature` → `sensor` | 39.125 | `/devices/hwmon/controls/Board Temperature` |
| CPU Temperature | `temperature` → `sensor` | 67.235 | `/devices/hwmon/controls/CPU Temperature` |

### `knx` — Шлюз KNX (1)

| control | type → class | value | topic |
|---|---|---|---|
| data | `text` → `raw` | i:0/0/0 i:0/0/0 GroupValueRead 0x00 | `/devices/knx/controls/data` |

### `metrics` — Метрики (13)

| control | type → class | value | topic |
|---|---|---|---|
| data_total_space | `value` → `sensor` | 56162 | `/devices/metrics/controls/data_total_space` |
| data_used_space | `value` → `sensor` | 2137 | `/devices/metrics/controls/data_used_space` |
| dev_root_linked_on | `text` → `raw` | /dev/mmcblk0p2 | `/devices/metrics/controls/dev_root_linked_on` |
| dev_root_total_space | `value` → `sensor` | 1984 | `/devices/metrics/controls/dev_root_total_space` |
| dev_root_used_space | `value` → `sensor` | 771 | `/devices/metrics/controls/dev_root_used_space` |
| load_average_15min | `value` → `sensor` | 0.74 | `/devices/metrics/controls/load_average_15min` |
| load_average_1min | `value` → `sensor` | 1.54 | `/devices/metrics/controls/load_average_1min` |
| load_average_5min | `value` → `sensor` | 0.91 | `/devices/metrics/controls/load_average_5min` |
| ram_available | `value` → `sensor` | 1611 | `/devices/metrics/controls/ram_available` |
| ram_total | `value` → `sensor` | 2012 | `/devices/metrics/controls/ram_total` |
| ram_used | `value` → `sensor` | 352 | `/devices/metrics/controls/ram_used` |
| swap_total | `value` → `sensor` | 255 | `/devices/metrics/controls/swap_total` |
| swap_used | `value` → `sensor` | 0 | `/devices/metrics/controls/swap_used` |

### `network` — Сеть (18)

| control | type → class | value | topic |
|---|---|---|---|
| Active Connections | `text` → `raw` | ["lo","wb-eth0"] | `/devices/network/controls/Active Connections` |
| Default Interface | `text` → `raw` | eth0 | `/devices/network/controls/Default Interface` |
| Ethernet 2 IP | `text` → `raw` |  | `/devices/network/controls/Ethernet 2 IP` |
| Ethernet 2 IP Connection Enabled | `switch` → `switch` | 0 | `/devices/network/controls/Ethernet 2 IP Connection Enabled` |
| Ethernet 2 IP Online Status | `switch` → `switch` | 0 | `/devices/network/controls/Ethernet 2 IP Online Status` |
| Ethernet IP | `text` → `raw` | 192.168.1.2
 | `/devices/network/controls/Ethernet IP` |
| Ethernet IP Connection Enabled | `switch` → `switch` | 1 | `/devices/network/controls/Ethernet IP Connection Enabled` |
| Ethernet IP Online Status | `switch` → `switch` | 1 | `/devices/network/controls/Ethernet IP Online Status` |
| GPRS IP | `text` → `raw` |  | `/devices/network/controls/GPRS IP` |
| GPRS IP Connection Enabled | `switch` → `switch` | 0 | `/devices/network/controls/GPRS IP Connection Enabled` |
| GPRS IP Online Status | `switch` → `switch` | 0 | `/devices/network/controls/GPRS IP Online Status` |
| Internet Connection | `text` → `raw` | wb-eth0 | `/devices/network/controls/Internet Connection` |
| Wi-Fi 2 IP | `text` → `raw` |  | `/devices/network/controls/Wi-Fi 2 IP` |
| Wi-Fi 2 IP Connection Enabled | `switch` → `switch` | 0 | `/devices/network/controls/Wi-Fi 2 IP Connection Enabled` |
| Wi-Fi 2 IP Online Status | `switch` → `switch` | 0 | `/devices/network/controls/Wi-Fi 2 IP Online Status` |
| Wi-Fi IP | `text` → `raw` |  | `/devices/network/controls/Wi-Fi IP` |
| Wi-Fi IP Connection Enabled | `switch` → `switch` | 0 | `/devices/network/controls/Wi-Fi IP Connection Enabled` |
| Wi-Fi IP Online Status | `switch` → `switch` | 0 | `/devices/network/controls/Wi-Fi IP Online Status` |

### `power_status` — Статус питания (2)

| control | type → class | value | topic |
|---|---|---|---|
| Vin | `voltage` → `sensor` | 20.4 | `/devices/power_status/controls/Vin` |
| working on battery | `switch` → `switch` | 0 | `/devices/power_status/controls/working on battery` |

### `system` — Система (10)

| control | type → class | value | topic |
|---|---|---|---|
| Batch No | `text` → `raw` | 7.3.3C/7 655 | `/devices/system/controls/Batch No` |
| Current uptime | `text` → `raw` | 19d 11h 45m | `/devices/system/controls/Current uptime` |
| DTS Version | `text` → `raw` | 733
 | `/devices/system/controls/DTS Version` |
| HW Revision | `text` → `raw` | 7.3.3 | `/devices/system/controls/HW Revision` |
| Manufacturing Date | `text` → `raw` | 2022-10-31 12:05:57 | `/devices/system/controls/Manufacturing Date` |
| Reboot | `pushbutton` → `switch` |  | `/devices/system/controls/Reboot` |
| Release name | `text` → `raw` | wb-2501 | `/devices/system/controls/Release name` |
| Release suite | `text` → `raw` | stable | `/devices/system/controls/Release suite` |
| Short SN | `text` → `raw` | AZBDNV3C | `/devices/system/controls/Short SN` |
| Temperature Grade | `text` → `raw` | commercial | `/devices/system/controls/Temperature Grade` |

### `system__networks__0f098677-2b49-4167-a534-207567b1751b` — system__networks__0f098677-2b49-4167-a534-207567b1751b (9)

| control | type → class | value | topic |
|---|---|---|---|
| Active | `switch` → `switch` | 0 | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/Active` |
| Address | `text` → `raw` |  | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/Address` |
| Connectivity | `switch` → `switch` | 0 | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/Connectivity` |
| Device | `text` → `raw` |  | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/Device` |
| Name | `text` → `raw` | wb-debug | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/Name` |
| State | `text` → `raw` | deactivated | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/State` |
| Type | `text` → `raw` | 802-3-ethernet | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/Type` |
| UUID | `text` → `raw` | 0f098677-2b49-4167-a534-207567b1751b | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/UUID` |
| UpDown | `pushbutton` → `switch` |  | `/devices/system__networks__0f098677-2b49-4167-a534-207567b1751b/controls/UpDown` |

### `system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac` — system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac (9)

| control | type → class | value | topic |
|---|---|---|---|
| Active | `switch` → `switch` | 0 | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/Active` |
| Address | `text` → `raw` |  | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/Address` |
| Connectivity | `switch` → `switch` | 0 | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/Connectivity` |
| Device | `text` → `raw` |  | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/Device` |
| Name | `text` → `raw` | MGTS_GPON_FFD5 | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/Name` |
| State | `text` → `raw` | deactivated | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/State` |
| Type | `text` → `raw` | 802-11-wireless | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/Type` |
| UUID | `text` → `raw` | 89adb663-5456-4fb5-9bf7-f815e3d8bbac | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/UUID` |
| UpDown | `pushbutton` → `switch` |  | `/devices/system__networks__89adb663-5456-4fb5-9bf7-f815e3d8bbac/controls/UpDown` |

### `system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e` — system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e (9)

| control | type → class | value | topic |
|---|---|---|---|
| Active | `switch` → `switch` | 1 | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/Active` |
| Address | `text` → `raw` | 192.168.1.2 | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/Address` |
| Connectivity | `switch` → `switch` | 1 | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/Connectivity` |
| Device | `text` → `raw` | eth0 | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/Device` |
| Name | `text` → `raw` | wb-eth0 | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/Name` |
| State | `text` → `raw` | activated | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/State` |
| Type | `text` → `raw` | 802-3-ethernet | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/Type` |
| UUID | `text` → `raw` | 91f1c71d-2d97-4675-886f-ecbe52b8451e | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/UUID` |
| UpDown | `pushbutton` → `switch` |  | `/devices/system__networks__91f1c71d-2d97-4675-886f-ecbe52b8451e/controls/UpDown` |

### `system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718` — system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718 (9)

| control | type → class | value | topic |
|---|---|---|---|
| Active | `switch` → `switch` | 1 | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/Active` |
| Address | `text` → `raw` | 127.0.0.1 | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/Address` |
| Connectivity | `switch` → `switch` | 1 | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/Connectivity` |
| Device | `text` → `raw` | lo | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/Device` |
| Name | `text` → `raw` | lo | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/Name` |
| State | `text` → `raw` | activated | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/State` |
| Type | `text` → `raw` | loopback | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/Type` |
| UUID | `text` → `raw` | a5e01d8c-0857-40c8-a920-f24a171f0718 | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/UUID` |
| UpDown | `pushbutton` → `switch` |  | `/devices/system__networks__a5e01d8c-0857-40c8-a920-f24a171f0718/controls/UpDown` |

### `system__networks__c3e38405-9c17-4155-ad70-664311b49066` — system__networks__c3e38405-9c17-4155-ad70-664311b49066 (9)

| control | type → class | value | topic |
|---|---|---|---|
| Active | `switch` → `switch` | 0 | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/Active` |
| Address | `text` → `raw` |  | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/Address` |
| Connectivity | `switch` → `switch` | 0 | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/Connectivity` |
| Device | `text` → `raw` |  | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/Device` |
| Name | `text` → `raw` | wb-eth1 | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/Name` |
| State | `text` → `raw` | deactivated | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/State` |
| Type | `text` → `raw` | 802-3-ethernet | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/Type` |
| UUID | `text` → `raw` | c3e38405-9c17-4155-ad70-664311b49066 | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/UUID` |
| UpDown | `pushbutton` → `switch` |  | `/devices/system__networks__c3e38405-9c17-4155-ad70-664311b49066/controls/UpDown` |

### `system__wb-cloud-agent__default` — system__wb-cloud-agent__default (3)

| control | type → class | value | topic |
|---|---|---|---|
| activation_link | `text` → `raw` | https://wirenboard.cloud/controllers?add=f20590e2c0bec731ded… | `/devices/system__wb-cloud-agent__default/controls/activation_link` |
| cloud_base_url | `text` → `raw` | https://wirenboard.cloud | `/devices/system__wb-cloud-agent__default/controls/cloud_base_url` |
| status | `text` → `raw` | ok | `/devices/system__wb-cloud-agent__default/controls/status` |

### `wb-adc` — ADCs (4)

| control | type → class | value | topic |
|---|---|---|---|
| A1 | `voltage` → `sensor` | 0.2 | `/devices/wb-adc/controls/A1` |
| A2 | `voltage` → `sensor` | 0.1 | `/devices/wb-adc/controls/A2` |
| A3 | `voltage` → `sensor` | 0.1 | `/devices/wb-adc/controls/A3` |
| Vin | `voltage` → `sensor` | 20.4 | `/devices/wb-adc/controls/Vin` |

### `wb-gpio` — Relays & FETs (18)

| control | type → class | value | topic |
|---|---|---|---|
| 5V_OUT | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/5V_OUT` |
| A1_IN | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/A1_IN` |
| A1_OUT | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/A1_OUT` |
| A2_IN | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/A2_IN` |
| A2_OUT | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/A2_OUT` |
| A3_IN | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/A3_IN` |
| A3_OUT | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/A3_OUT` |
| D1_IN | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/D1_IN` |
| D1_OUT | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/D1_OUT` |
| EXT1_K1 | `switch` → `switch` | 1 | `/devices/wb-gpio/controls/EXT1_K1` |
| EXT1_K2 | `switch` → `switch` | 1 | `/devices/wb-gpio/controls/EXT1_K2` |
| EXT1_K3 | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K3` |
| EXT1_K4 | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K4` |
| EXT1_K5 | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K5` |
| EXT1_K6 | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K6` |
| EXT1_K7 | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K7` |
| EXT1_K8 | `switch` → `switch` | 0 | `/devices/wb-gpio/controls/EXT1_K8` |
| V_OUT | `switch` → `switch` | 1 | `/devices/wb-gpio/controls/V_OUT` |

### `wb-led_221` — WB-LED 221 (21)

| control | type → class | value | topic |
|---|---|---|---|
| Allowed Power | `value` → `sensor` | 100 | `/devices/wb-led_221/controls/Allowed Power` |
| Board Temperature | `value` → `sensor` | 30 | `/devices/wb-led_221/controls/Board Temperature` |
| Channel 4 | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Channel 4` |
| Channel 4 Brightness | `range` → `range` | 100 | `/devices/wb-led_221/controls/Channel 4 Brightness` |
| Hue Changing | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Hue Changing` |
| Hue Changing Rate | `value` → `sensor` | 30 | `/devices/wb-led_221/controls/Hue Changing Rate` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Input 1` |
| Input 1 Counter | `value` → `sensor` | 0 | `/devices/wb-led_221/controls/Input 1 Counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Input 2` |
| Input 2 Counter | `value` → `sensor` | 0 | `/devices/wb-led_221/controls/Input 2 Counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Input 3` |
| Input 3 Counter | `value` → `sensor` | 0 | `/devices/wb-led_221/controls/Input 3 Counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Input 4` |
| Input 4 Counter | `value` → `sensor` | 0 | `/devices/wb-led_221/controls/Input 4 Counter` |
| Overcurrent | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/Overcurrent` |
| RGB Palette | `rgb` → `raw` | 76;0;130 | `/devices/wb-led_221/controls/RGB Palette` |
| RGB Strip | `switch` → `switch` | 0 | `/devices/wb-led_221/controls/RGB Strip` |
| RGB Strip Brightness | `range` → `range` | 51 | `/devices/wb-led_221/controls/RGB Strip Brightness` |
| RGB Strip Hue | `range` → `range` | 275 | `/devices/wb-led_221/controls/RGB Strip Hue` |
| RGB Strip Saturation | `range` → `range` | 100 | `/devices/wb-led_221/controls/RGB Strip Saturation` |
| Serial | `text` → `raw` | 105855 | `/devices/wb-led_221/controls/Serial` |

### `wb-mdm3_218` — WB-MDM3 218 (33)

| control | type → class | value | topic |
|---|---|---|---|
| AC on L-N | `switch` → `switch` | 1 | `/devices/wb-mdm3_218/controls/AC on L-N` |
| Channel 1 | `range` → `range` | 100 | `/devices/wb-mdm3_218/controls/Channel 1` |
| Channel 2 | `range` → `range` | 100 | `/devices/wb-mdm3_218/controls/Channel 2` |
| Channel 3 | `range` → `range` | 100 | `/devices/wb-mdm3_218/controls/Channel 3` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Input 1` |
| Input 1 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 1 Long Press Counter` |
| Input 1 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 1 Single Press Counter` |
| Input 1 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 1 counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Input 2` |
| Input 2 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 2 Long Press Counter` |
| Input 2 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 2 Single Press Counter` |
| Input 2 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 2 counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Input 3` |
| Input 3 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 3 Long Press Counter` |
| Input 3 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 3 Single Press Counter` |
| Input 3 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 3 counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Input 4` |
| Input 4 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 4 Long Press Counter` |
| Input 4 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 4 Single Press Counter` |
| Input 4 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 4 counter` |
| Input 5 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Input 5` |
| Input 5 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 5 Long Press Counter` |
| Input 5 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 5 Single Press Counter` |
| Input 5 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 5 counter` |
| Input 6 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Input 6` |
| Input 6 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 6 Long Press Counter` |
| Input 6 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 6 Single Press Counter` |
| Input 6 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_218/controls/Input 6 counter` |
| K1 | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/K1` |
| K2 | `switch` → `switch` | 1 | `/devices/wb-mdm3_218/controls/K2` |
| K3 | `switch` → `switch` | 1 | `/devices/wb-mdm3_218/controls/K3` |
| Overcurrent | `switch` → `switch` | 0 | `/devices/wb-mdm3_218/controls/Overcurrent` |
| Serial | `text` → `raw` | 431016 | `/devices/wb-mdm3_218/controls/Serial` |

### `wb-mdm3_75` — WB-MDM3 75 (33)

| control | type → class | value | topic |
|---|---|---|---|
| AC on L-N | `switch` → `switch` | 1 | `/devices/wb-mdm3_75/controls/AC on L-N` |
| Channel 1 | `range` → `range` | 100 | `/devices/wb-mdm3_75/controls/Channel 1` |
| Channel 2 | `range` → `range` | 100 | `/devices/wb-mdm3_75/controls/Channel 2` |
| Channel 3 | `range` → `range` | 100 | `/devices/wb-mdm3_75/controls/Channel 3` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Input 1` |
| Input 1 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 1 Long Press Counter` |
| Input 1 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 1 Single Press Counter` |
| Input 1 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 1 counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Input 2` |
| Input 2 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 2 Long Press Counter` |
| Input 2 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 2 Single Press Counter` |
| Input 2 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 2 counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Input 3` |
| Input 3 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 3 Long Press Counter` |
| Input 3 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 3 Single Press Counter` |
| Input 3 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 3 counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Input 4` |
| Input 4 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 4 Long Press Counter` |
| Input 4 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 4 Single Press Counter` |
| Input 4 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 4 counter` |
| Input 5 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Input 5` |
| Input 5 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 5 Long Press Counter` |
| Input 5 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 5 Single Press Counter` |
| Input 5 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 5 counter` |
| Input 6 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Input 6` |
| Input 6 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 6 Long Press Counter` |
| Input 6 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 6 Single Press Counter` |
| Input 6 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_75/controls/Input 6 counter` |
| K1 | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/K1` |
| K2 | `switch` → `switch` | 1 | `/devices/wb-mdm3_75/controls/K2` |
| K3 | `switch` → `switch` | 1 | `/devices/wb-mdm3_75/controls/K3` |
| Overcurrent | `switch` → `switch` | 0 | `/devices/wb-mdm3_75/controls/Overcurrent` |
| Serial | `text` → `raw` | 169332 | `/devices/wb-mdm3_75/controls/Serial` |

### `wb-mdm3_76` — WB-MDM3 76 (33)

| control | type → class | value | topic |
|---|---|---|---|
| AC on L-N | `switch` → `switch` | 1 | `/devices/wb-mdm3_76/controls/AC on L-N` |
| Channel 1 | `range` → `range` | 100 | `/devices/wb-mdm3_76/controls/Channel 1` |
| Channel 2 | `range` → `range` | 100 | `/devices/wb-mdm3_76/controls/Channel 2` |
| Channel 3 | `range` → `range` | 100 | `/devices/wb-mdm3_76/controls/Channel 3` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Input 1` |
| Input 1 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 1 Long Press Counter` |
| Input 1 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 1 Single Press Counter` |
| Input 1 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 1 counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Input 2` |
| Input 2 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 2 Long Press Counter` |
| Input 2 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 2 Single Press Counter` |
| Input 2 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 2 counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Input 3` |
| Input 3 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 3 Long Press Counter` |
| Input 3 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 3 Single Press Counter` |
| Input 3 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 3 counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Input 4` |
| Input 4 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 4 Long Press Counter` |
| Input 4 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 4 Single Press Counter` |
| Input 4 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 4 counter` |
| Input 5 | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Input 5` |
| Input 5 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 5 Long Press Counter` |
| Input 5 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 5 Single Press Counter` |
| Input 5 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 5 counter` |
| Input 6 | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Input 6` |
| Input 6 Long Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 6 Long Press Counter` |
| Input 6 Single Press Counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 6 Single Press Counter` |
| Input 6 counter | `value` → `sensor` | 0 | `/devices/wb-mdm3_76/controls/Input 6 counter` |
| K1 | `switch` → `switch` | 1 | `/devices/wb-mdm3_76/controls/K1` |
| K2 | `switch` → `switch` | 1 | `/devices/wb-mdm3_76/controls/K2` |
| K3 | `switch` → `switch` | 1 | `/devices/wb-mdm3_76/controls/K3` |
| Overcurrent | `switch` → `switch` | 0 | `/devices/wb-mdm3_76/controls/Overcurrent` |
| Serial | `text` → `raw` | 169333 | `/devices/wb-mdm3_76/controls/Serial` |

### `wb-mr6cu_43` — WB-MR6CU 43 (7)

| control | type → class | value | topic |
|---|---|---|---|
| K1 | `switch` → `switch` | 1 | `/devices/wb-mr6cu_43/controls/K1` |
| K2 | `switch` → `switch` | 0 | `/devices/wb-mr6cu_43/controls/K2` |
| K3 | `switch` → `switch` | 0 | `/devices/wb-mr6cu_43/controls/K3` |
| K4 | `switch` → `switch` | 0 | `/devices/wb-mr6cu_43/controls/K4` |
| K5 | `switch` → `switch` | 0 | `/devices/wb-mr6cu_43/controls/K5` |
| K6 | `switch` → `switch` | 0 | `/devices/wb-mr6cu_43/controls/K6` |
| Serial | `text` → `raw` | 4262603572 | `/devices/wb-mr6cu_43/controls/Serial` |

### `wb-mr6cv3_225` — WB-MR6C v.3 225 (22)

| control | type → class | value | topic |
|---|---|---|---|
| Bus Power Supply | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/Bus Power Supply` |
| Input 0 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 0` |
| Input 0 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 0 counter` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 1` |
| Input 1 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 1 counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 2` |
| Input 2 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 2 counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 3` |
| Input 3 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 3 counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 4` |
| Input 4 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 4 counter` |
| Input 5 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 5` |
| Input 5 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 5 counter` |
| Input 6 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_225/controls/Input 6` |
| Input 6 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_225/controls/Input 6 counter` |
| K1 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K1` |
| K2 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K2` |
| K3 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K3` |
| K4 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K4` |
| K5 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K5` |
| K6 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_225/controls/K6` |
| Serial | `text` → `raw` | 161672 | `/devices/wb-mr6cv3_225/controls/Serial` |

### `wb-mr6cv3_227` — WB-MR6C v.3 227 (22)

| control | type → class | value | topic |
|---|---|---|---|
| Bus Power Supply | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_227/controls/Bus Power Supply` |
| Input 0 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 0` |
| Input 0 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 0 counter` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 1` |
| Input 1 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 1 counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 2` |
| Input 2 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 2 counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 3` |
| Input 3 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 3 counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 4` |
| Input 4 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 4 counter` |
| Input 5 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 5` |
| Input 5 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 5 counter` |
| Input 6 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/Input 6` |
| Input 6 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_227/controls/Input 6 counter` |
| K1 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_227/controls/K1` |
| K2 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K2` |
| K3 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K3` |
| K4 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K4` |
| K5 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K5` |
| K6 | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_227/controls/K6` |
| Serial | `text` → `raw` | 161680 | `/devices/wb-mr6cv3_227/controls/Serial` |

### `wb-mr6cv3_239` — WB-MR6C v.3 239 (22)

| control | type → class | value | topic |
|---|---|---|---|
| Bus Power Supply | `switch` → `switch` | 1 | `/devices/wb-mr6cv3_239/controls/Bus Power Supply` |
| Input 0 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 0` |
| Input 0 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 0 counter` |
| Input 1 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 1` |
| Input 1 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 1 counter` |
| Input 2 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 2` |
| Input 2 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 2 counter` |
| Input 3 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 3` |
| Input 3 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 3 counter` |
| Input 4 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 4` |
| Input 4 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 4 counter` |
| Input 5 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 5` |
| Input 5 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 5 counter` |
| Input 6 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/Input 6` |
| Input 6 counter | `value` → `sensor` | 0 | `/devices/wb-mr6cv3_239/controls/Input 6 counter` |
| K1 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K1` |
| K2 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K2` |
| K3 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K3` |
| K4 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K4` |
| K5 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K5` |
| K6 | `switch` → `switch` | 0 | `/devices/wb-mr6cv3_239/controls/K6` |
| Serial | `text` → `raw` | 161739 | `/devices/wb-mr6cv3_239/controls/Serial` |

### `wbrules` — Настройки движка правил (1)

| control | type → class | value | topic |
|---|---|---|---|
| Rule debugging | `switch` → `switch` |  | `/devices/wbrules/controls/Rule debugging` |

