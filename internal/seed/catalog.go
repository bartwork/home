package seed

import "github.com/bartwork/home/internal/layout"

// Данные квартиры из docs/devices.md — стартовый каталог.
// Универсальный UI комнаты/устройства правит дальше вручную.

type RoomDef struct {
	Name       string
	X, Y, W, H float64
	SortOrder  int64
}

type DeviceDef struct {
	Name       string
	Room       string // имя комнаты; пусто = без комнаты
	Type       string // switch|range|sensor|climate|led|floor|raw
	Topic      string
	PowerTopic string // явно; пусто: range → InferPowerTopic, floor → FloorSensorTopic
	Icon       string
	Unit       string
}

var Rooms = []RoomDef{
	{Name: "коридор", X: 1, Y: 1, W: 32, H: 28, SortOrder: 1},
	{Name: "комната", X: 34, Y: 1, W: 40, H: 48, SortOrder: 2},
	{Name: "кухня", X: 75, Y: 1, W: 24, H: 48, SortOrder: 3},
	{Name: "кладовка", X: 1, Y: 31, W: 32, H: 28, SortOrder: 4},
	{Name: "ванна", X: 34, Y: 51, W: 32, H: 28, SortOrder: 5},
	{Name: "туалет", X: 67, Y: 51, W: 32, H: 28, SortOrder: 6},
}

// Devices — топики квартиры. MDM3 power — InferPowerTopic; лента — тип led (якорь RGB Strip).
var Devices = []DeviceDef{
	{Name: "основной свет", Room: "коридор", Type: "range", Topic: "/devices/wb-mdm3_76/controls/Channel 1", Icon: "dimmer"},
	{Name: "мастер-выключатель", Room: "", Type: "switch", Topic: "/devices/wb-mr6cu_43/controls/K1", Icon: "switch"},
	{Name: "основной свет", Room: "комната", Type: "range", Topic: "/devices/wb-mdm3_75/controls/Channel 1", Icon: "dimmer"},
	{Name: "лоджия свет", Room: "комната", Type: "range", Topic: "/devices/wb-mdm3_75/controls/Channel 3", Icon: "light"},
	{Name: "лента", Room: "кладовка", Type: "switch", Topic: "/devices/wb-mr6cv3_239/controls/K4", Icon: "light"},
	{Name: "бра около кладовки", Room: "комната", Type: "range", Topic: "/devices/wb-mdm3_76/controls/Channel 2", Icon: "dimmer"},
	{Name: "бра около лоджии", Room: "комната", Type: "range", Topic: "/devices/wb-mdm3_76/controls/Channel 3", Icon: "dimmer"},
	{Name: "свет", Room: "кладовка", Type: "switch", Topic: "/devices/wb-mr6cv3_239/controls/K5", Icon: "light"},
	{Name: "основной свет", Room: "ванна", Type: "switch", Topic: "/devices/wb-mr6cv3_227/controls/K1", Icon: "light"},
	{Name: "зеркало", Room: "ванна", Type: "switch", Topic: "/devices/wb-mr6cv3_227/controls/K2", Icon: "light"},
	{Name: "вытяжка", Room: "ванна", Type: "switch", Topic: "/devices/wb-mr6cv3_227/controls/K3", Icon: "fan"},
	{Name: "зеркало", Room: "туалет", Type: "switch", Topic: "/devices/wb-mr6cv3_227/controls/K4", Icon: "light"},
	{Name: "свет", Room: "туалет", Type: "switch", Topic: "/devices/wb-mr6cv3_227/controls/K5", Icon: "light"},
	{Name: "вытяжка", Room: "туалет", Type: "switch", Topic: "/devices/wb-mr6cv3_227/controls/K6", Icon: "fan"},
	{Name: "тёплый пол", Room: "коридор", Type: "floor", Topic: "/devices/wb-mr6cv3_225/controls/K1", Icon: "thermo"},
	{Name: "тёплый пол", Room: "туалет", Type: "floor", Topic: "/devices/wb-mr6cv3_225/controls/K2", Icon: "thermo"},
	{Name: "тёплый пол", Room: "ванна", Type: "floor", Topic: "/devices/wb-mr6cv3_225/controls/K4", Icon: "thermo"},
	{Name: "тёплый пол", Room: "кухня", Type: "floor", Topic: "/devices/wb-mr6cv3_225/controls/K5", Icon: "thermo"},
	{Name: "дополнительный свет", Room: "комната", Type: "range", Topic: "/devices/wb-mdm3_75/controls/Channel 2", Icon: "dimmer"},
	{Name: "лента карниза", Room: "комната", Type: "led", Topic: "/devices/wb-led_221/controls/RGB Strip", Icon: "light"},
	{Name: "свет у окна", Room: "кухня", Type: "range", Topic: "/devices/wb-mdm3_218/controls/Channel 1", Icon: "dimmer"},
	{Name: "лампа", Room: "кухня", Type: "range", Topic: "/devices/wb-mdm3_218/controls/Channel 2", Icon: "dimmer"},
	{Name: "свет у двери", Room: "кухня", Type: "range", Topic: "/devices/wb-mdm3_218/controls/Channel 3", Icon: "dimmer"},
	{Name: "бра", Room: "кухня", Type: "switch", Topic: "/devices/wb-mr6cv3_239/controls/K1", Icon: "light"},
	{Name: "подсветка кухни", Room: "кухня", Type: "switch", Topic: "/devices/wb-mr6cv3_239/controls/K2", Icon: "light"},
	{Name: "вытяжка", Room: "кухня", Type: "switch", Topic: "/devices/wb-mr6cv3_239/controls/K3", Icon: "fan"},
	{Name: "кондиционер", Room: "кухня", Type: "climate", Topic: "/devices/ONOKOM-AIR-HR-1-MB-B_1/controls/Active", Icon: "ac"},
	{Name: "кондиционер", Room: "комната", Type: "climate", Topic: "/devices/ONOKOM-AIR-HR-1-MB-B_2/controls/Active", Icon: "ac"},
}

func powerFor(d DeviceDef) string {
	if d.PowerTopic != "" {
		return d.PowerTopic
	}
	if d.Type == "range" {
		return layout.InferPowerTopic(d.Topic)
	}
	if d.Type == "floor" {
		return layout.FloorSensorTopic(d.Topic)
	}
	return ""
}
