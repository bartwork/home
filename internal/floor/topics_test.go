package floor

import "testing"

func TestTopics(t *testing.T) {
	if got := SetpointTopic(3); got != "/devices/home/controls/floor_3_setpoint" {
		t.Fatalf("setpoint: %s", got)
	}
	if got := ModeTopic(3); got != "/devices/home/controls/floor_3_mode" {
		t.Fatalf("mode: %s", got)
	}
	state, ok := StateFromCommand("/devices/home/controls/floor_3_setpoint/on")
	if !ok || state != "/devices/home/controls/floor_3_setpoint" {
		t.Fatalf("cmd: %s %v", state, ok)
	}
	if _, ok := StateFromCommand("/devices/wb-mr6cv3_225/controls/K1/on"); ok {
		t.Fatal("wb should not be home")
	}
	id, ok := ParseDeviceID("/devices/home/controls/floor_12_mode")
	if !ok || id != 12 {
		t.Fatalf("parse: %d %v", id, ok)
	}
}
