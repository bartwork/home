package air

import "strings"

const (
	ControlTemperature = "Temperature"
	ControlHumidity    = "Humidity"
	ControlCO2         = "CO2"
	ControlVOC         = "Air Quality (VOC)"
	ControlIlluminance = "Illuminance"
	ControlSound       = "Sound Level"
	ControlMotion      = "Current Motion"
	ControlMaxMotion   = "Max Motion"
	ControlRedLED      = "Red LED"
	ControlGreenLED    = "Green LED"
)

// SiblingControl: …/controls/<якорь> → …/controls/<control>.
func SiblingControl(mqttTopic, control string) string {
	topic := strings.TrimSpace(mqttTopic)
	marker := "/controls/"
	idx := strings.LastIndex(topic, marker)
	if idx < 0 || strings.TrimSpace(control) == "" {
		return ""
	}
	return topic[:idx+len(marker)] + control
}

func CO2Topic(anchor string) string       { return SiblingControl(anchor, ControlCO2) }
func RedLEDTopic(anchor string) string    { return SiblingControl(anchor, ControlRedLED) }
func GreenLEDTopic(anchor string) string  { return SiblingControl(anchor, ControlGreenLED) }

// ParseMSWCO2: /devices/wb-msw…/controls/CO2 → deviceID.
func ParseMSWCO2(topic string) (deviceID string, ok bool) {
	topic = strings.TrimSpace(topic)
	const prefix = "/devices/"
	if !strings.HasPrefix(topic, prefix) {
		return "", false
	}
	rest := strings.TrimPrefix(topic, prefix)
	slash := strings.IndexByte(rest, '/')
	if slash <= 0 {
		return "", false
	}
	deviceID = rest[:slash]
	if !IsMSWDevice(deviceID) {
		return "", false
	}
	if rest[slash:] != "/controls/"+ControlCO2 {
		return "", false
	}
	return deviceID, true
}

func IsMSWDevice(deviceID string) bool {
	id := strings.ToLower(strings.TrimSpace(deviceID))
	return strings.HasPrefix(id, "wb-msw")
}

// AnchorFromDeviceID — якорный топик Temperature для UI/LED.
func AnchorFromDeviceID(deviceID string) string {
	id := strings.TrimSpace(deviceID)
	if id == "" {
		return ""
	}
	return "/devices/" + id + "/controls/" + ControlTemperature
}
