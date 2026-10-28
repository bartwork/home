package air

import "testing"

func TestSiblingAndLEDTopics(t *testing.T) {
	anchor := "/devices/wb-msw-v3_108/controls/Temperature"
	if got := CO2Topic(anchor); got != "/devices/wb-msw-v3_108/controls/CO2" {
		t.Fatalf("co2: %s", got)
	}
	if got := RedLEDTopic(anchor); got != "/devices/wb-msw-v3_108/controls/Red LED" {
		t.Fatalf("red: %s", got)
	}
	if got := GreenLEDTopic(anchor); got != "/devices/wb-msw-v3_108/controls/Green LED" {
		t.Fatalf("green: %s", got)
	}
}

func TestParseMSWCO2(t *testing.T) {
	id, ok := ParseMSWCO2("/devices/wb-msw-v3_108/controls/CO2")
	if !ok || id != "wb-msw-v3_108" {
		t.Fatalf("got %q %v", id, ok)
	}
	if _, ok := ParseMSWCO2("/devices/wb-msw-v3_108/controls/Temperature"); ok {
		t.Fatal("temperature is not co2")
	}
	if _, ok := ParseMSWCO2("/devices/wb-mr6cv3_225/controls/CO2"); ok {
		t.Fatal("non-msw")
	}
}
