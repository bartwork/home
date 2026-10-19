package layout

import "strings"

// Реле тёплого пола → датчик температуры (каталог квартиры).
var floorRelaySensors = map[string]string{
	"/devices/wb-mr6cv3_225/controls/K1": "/devices/wb-m1w2_83/controls/External Sensor 1", // коридор
	"/devices/wb-mr6cv3_225/controls/K2": "/devices/wb-m1w2_83/controls/External Sensor 2", // туалет
	"/devices/wb-mr6cv3_225/controls/K4": "/devices/wb-m1w2_82/controls/External Sensor 1", // ванна
	"/devices/wb-mr6cv3_225/controls/K5": "/devices/wb-m1w2_82/controls/External Sensor 2", // кухня
}

var floorSensorTopics = func() map[string]struct{} {
	out := make(map[string]struct{}, len(floorRelaySensors))
	for _, sensor := range floorRelaySensors {
		out[sensor] = struct{}{}
	}
	return out
}()

// FloorSensorTopic — датчик температуры для известного реле тёплого пола.
// Пусто, если топик не из каталога квартиры.
func FloorSensorTopic(relayTopic string) string {
	return floorRelaySensors[strings.TrimSpace(relayTopic)]
}

// IsFloorRelay — реле тёплого пола из каталога.
func IsFloorRelay(deviceID, control string) bool {
	return FloorSensorTopic(controlTopic(deviceID, control)) != ""
}

// IsFloorTempSensor — датчик, привязанный к тёплому полу (sibling → raw в discovery).
func IsFloorTempSensor(deviceID, control string) bool {
	_, ok := floorSensorTopics[controlTopic(deviceID, control)]
	return ok
}

func controlTopic(deviceID, control string) string {
	return "/devices/" + strings.TrimSpace(deviceID) + "/controls/" + strings.TrimSpace(control)
}
