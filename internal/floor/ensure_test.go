package floor

import "testing"

func TestRelaysInGraph(t *testing.T) {
	raw := `{
		"nodes":[
			{"id":"t","type":"trigger.mqtt","data":{"topic":"/x"}},
			{"id":"th","type":"logic.thermostat","data":{"relay":"/devices/wb-mr6cv3_225/controls/K1"}}
		]
	}`
	relays := relaysInGraph(raw)
	if _, ok := relays["/devices/wb-mr6cv3_225/controls/K1"]; !ok {
		t.Fatalf("relays: %v", relays)
	}
}

func TestFloorDeviceID(t *testing.T) {
	id, ok := floorDeviceID(`{"meta":{"floorDeviceId":9},"nodes":[]}`)
	if !ok || id != 9 {
		t.Fatalf("%d %v", id, ok)
	}
	if _, ok := floorDeviceID(`{"nodes":[]}`); ok {
		t.Fatal("expected no meta")
	}
}
