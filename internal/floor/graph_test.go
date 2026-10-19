package floor

import (
	"encoding/json"
	"testing"
)

func TestThermostatGraphJSON(t *testing.T) {
	raw, err := ThermostatGraphJSON(7, "/devices/wb-mr6cv3_225/controls/K1", "/devices/wb-m1w2_83/controls/External Sensor 1")
	if err != nil {
		t.Fatal(err)
	}
	var g struct {
		Meta *struct {
			FloorDeviceID int64 `json:"floorDeviceId"`
		} `json:"meta"`
		Nodes []struct {
			ID   string `json:"id"`
			Type string `json:"type"`
		} `json:"nodes"`
		Edges []struct {
			ID string `json:"id"`
		} `json:"edges"`
	}
	if err := json.Unmarshal(raw, &g); err != nil {
		t.Fatal(err)
	}
	if g.Meta == nil || g.Meta.FloorDeviceID != 7 {
		t.Fatalf("meta: %+v", g.Meta)
	}
	if len(g.Nodes) != 4 || len(g.Edges) != 3 {
		t.Fatalf("nodes=%d edges=%d", len(g.Nodes), len(g.Edges))
	}
	types := map[string]string{}
	for _, n := range g.Nodes {
		types[n.ID] = n.Type
	}
	if types["thermo"] != "logic.thermostat" || types["t_temp"] != "trigger.mqtt" {
		t.Fatalf("types: %v", types)
	}
}
