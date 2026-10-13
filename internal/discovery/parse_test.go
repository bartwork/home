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

func TestParseMetaPayloadJSON(t *testing.T) {
	m := discovery.ParseMetaPayload(discovery.KindControlMetaJSON, "", `{"type":"switch","title":"Реле","units":""}`)
	if m.Type != "switch" || m.Title != "Реле" {
		t.Fatalf("%+v", m)
	}
}
