package discovery_test

import (
	"testing"

	"github.com/bartwork/home/internal/discovery"
)

func TestParseTopicState(t *testing.T) {
	p, ok := discovery.ParseTopic("/devices/wb-gpio/controls/K1")
	if !ok || p.Kind != discovery.KindState {
		t.Fatalf("got %+v ok=%v", p, ok)
	}
	if p.DeviceID != "wb-gpio" || p.Control != "K1" || p.StateTopic != "/devices/wb-gpio/controls/K1" {
		t.Fatalf("%+v", p)
	}
}

func TestParseTopicMetaJSON(t *testing.T) {
	p, ok := discovery.ParseTopic("/devices/wb-msw-v4/controls/Temperature/meta")
	if !ok || p.Kind != discovery.KindControlMetaJSON {
		t.Fatalf("got %+v", p)
	}
}

func TestParseTopicMetaField(t *testing.T) {
	p, ok := discovery.ParseTopic("/devices/wb-gpio/controls/K1/meta/type")
	if !ok || p.Kind != discovery.KindControlMetaField || p.MetaField != "type" {
		t.Fatalf("got %+v", p)
	}
}

func TestParseTopicIgnore(t *testing.T) {
	if _, ok := discovery.ParseTopic("/devices/wb-gpio/meta"); ok {
		t.Fatal("device meta should be ignored")
	}
	if _, ok := discovery.ParseTopic("/devices/wb-gpio/controls/K1/on"); ok {
		t.Fatal("on should be ignored")
	}
}

func TestClassifyWBType(t *testing.T) {
	if discovery.ClassifyWBType("switch") != "switch" {
		t.Fatal("switch")
	}
	if discovery.ClassifyWBType("temperature") != "sensor" {
		t.Fatal("temperature")
	}
	if discovery.ClassifyWBType("range") != "range" {
		t.Fatal("range")
	}
}

func TestSuggestDeviceTypeOnokom(t *testing.T) {
	if got := discovery.SuggestDeviceType("ONOKOM-AIR-HR-1-MB-B_1", "Active", "switch"); got != "climate" {
		t.Fatalf("Active: %q", got)
	}
	if got := discovery.SuggestDeviceType("ONOKOM-AIR-HR-1-MB-B_2", "Mode", "range"); got != "raw" {
		t.Fatalf("Mode sibling: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-mr6c_225", "K1", "switch"); got != "switch" {
		t.Fatalf("relay: %q", got)
	}
}

func TestSuggestDeviceTypeFloor(t *testing.T) {
	if got := discovery.SuggestDeviceType("wb-mr6cv3_225", "K1", "switch"); got != "floor" {
		t.Fatalf("floor relay: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-mr6cv3_225", "K5", "switch"); got != "floor" {
		t.Fatalf("floor kitchen: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-m1w2_83", "External Sensor 1", "temperature"); got != "raw" {
		t.Fatalf("floor sensor sibling: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-mr6cv3_227", "K1", "switch"); got != "switch" {
		t.Fatalf("other relay: %q", got)
	}
}

func TestSuggestDeviceTypeMSW(t *testing.T) {
	if got := discovery.SuggestDeviceType("wb-msw-v3_108", "Temperature", "temperature"); got != "air" {
		t.Fatalf("Temperature: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-msw-v3_108", "CO2", "concentration"); got != "raw" {
		t.Fatalf("CO2 sibling: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-msw-v3_108", "Humidity", "rel_humidity"); got != "raw" {
		t.Fatalf("Humidity sibling: %q", got)
	}
}

func TestSuggestDeviceTypeLed(t *testing.T) {
	if got := discovery.SuggestDeviceType("wb-led_221", "RGB Strip", "switch"); got != "led" {
		t.Fatalf("RGB Strip: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-led_221", "RGB Strip Brightness", "range"); got != "raw" {
		t.Fatalf("Brightness sibling: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-led_221", "RGB Strip Hue", "range"); got != "raw" {
		t.Fatalf("Hue sibling: %q", got)
	}
	if got := discovery.SuggestDeviceType("wb-led_221", "Channel 4", "switch"); got != "raw" {
		t.Fatalf("Channel 4 sibling: %q", got)
	}
}

func TestParseMetaPayloadJSON(t *testing.T) {
	m := discovery.ParseMetaPayload(discovery.KindControlMetaJSON, "", `{"type":"switch","title":"Реле","units":""}`)
	if m.Type != "switch" || m.Title != "Реле" {
		t.Fatalf("%+v", m)
	}
}
