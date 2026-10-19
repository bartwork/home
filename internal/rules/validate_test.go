package rules

import (
	_ "embed"
	"testing"
)

//go:embed examples/floor_thermostat.json
var floorExample []byte

func TestValidateFloorExample(t *testing.T) {
	if err := ValidateGraphJSON(string(floorExample)); err != nil {
		t.Fatal(err)
	}
}

func TestValidateGraphRejectsUnknown(t *testing.T) {
	err := ValidateGraphJSON(`{
		"nodes":[{"id":"t1","type":"trigger.mqtt","position":{"x":0,"y":0},"data":{"topic":"/devices/a/controls/b"}},
		{"id":"x","type":"magic.box","position":{"x":0,"y":0},"data":{}}],
		"edges":[{"id":"e1","source":"t1","target":"x"}]
	}`)
	if err == nil {
		t.Fatal("expected error")
	}
}

func TestCompileFloorExample(t *testing.T) {
	cr, err := compileRule(1, "floor", true, ModeSingle, 0, string(floorExample))
	if err != nil {
		t.Fatal(err)
	}
	if len(cr.mqttByTopic) != 3 {
		t.Fatalf("mqtt topics: %v", cr.mqttByTopic)
	}
}
