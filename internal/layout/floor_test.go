package layout_test

import (
	"testing"

	"github.com/bartwork/home/internal/layout"
)

func TestFloorSensorTopic(t *testing.T) {
	if got := layout.FloorSensorTopic("/devices/wb-mr6cv3_225/controls/K1"); got != "/devices/wb-m1w2_83/controls/External Sensor 1" {
		t.Fatalf("K1: %q", got)
	}
	if got := layout.FloorSensorTopic("/devices/wb-mr6cv3_225/controls/K5"); got != "/devices/wb-m1w2_82/controls/External Sensor 2" {
		t.Fatalf("K5: %q", got)
	}
	if got := layout.FloorSensorTopic("/devices/wb-mr6cv3_227/controls/K1"); got != "" {
		t.Fatalf("other: %q", got)
	}
}

func TestIsFloorTempSensor(t *testing.T) {
	if !layout.IsFloorTempSensor("wb-m1w2_83", "External Sensor 1") {
		t.Fatal("corridor sensor")
	}
	if layout.IsFloorTempSensor("wb-m1w2_83", "External Sensor 3") {
		t.Fatal("unknown sensor")
	}
}
