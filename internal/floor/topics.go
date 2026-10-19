package floor

import (
	"fmt"
	"strconv"
	"strings"
)

const (
	DeviceID = "home"

	DefaultSetpoint = "26"
	DefaultMode     = "auto"
	DefaultHyst     = 0.5

	ModeOff  = "off"
	ModeHeat = "heat"
	ModeAuto = "auto"
)

func SetpointTopic(deviceID int64) string {
	return fmt.Sprintf("/devices/%s/controls/floor_%d_setpoint", DeviceID, deviceID)
}

func ModeTopic(deviceID int64) string {
	return fmt.Sprintf("/devices/%s/controls/floor_%d_mode", DeviceID, deviceID)
}

func IsControlTopic(topic string) bool {
	prefix := "/devices/" + DeviceID + "/controls/"
	t := strings.TrimSpace(topic)
	if !strings.HasPrefix(t, prefix) {
		return false
	}
	rest := strings.TrimPrefix(t, prefix)
	return rest != "" && !strings.Contains(rest, "/")
}

// CommandTopic — WB write-топик: state → …/on.
func CommandTopic(stateTopic string) string {
	t := strings.TrimSpace(stateTopic)
	if t == "" || strings.HasSuffix(t, "/on") {
		return t
	}
	return t + "/on"
}

// StateFromCommand — если cmd …/on у home, вернуть state-топик.
func StateFromCommand(cmdTopic string) (stateTopic string, ok bool) {
	t := strings.TrimSpace(cmdTopic)
	if !strings.HasSuffix(t, "/on") {
		return "", false
	}
	state := strings.TrimSuffix(t, "/on")
	if !IsControlTopic(state) {
		return "", false
	}
	return state, true
}

func ParseDeviceID(topic string) (int64, bool) {
	prefix := "/devices/" + DeviceID + "/controls/floor_"
	t := strings.TrimSpace(topic)
	if !strings.HasPrefix(t, prefix) {
		return 0, false
	}
	rest := strings.TrimPrefix(t, prefix)
	var idPart string
	switch {
	case strings.HasSuffix(rest, "_setpoint"):
		idPart = strings.TrimSuffix(rest, "_setpoint")
	case strings.HasSuffix(rest, "_mode"):
		idPart = strings.TrimSuffix(rest, "_mode")
	default:
		return 0, false
	}
	id, err := strconv.ParseInt(idPart, 10, 64)
	if err != nil || id <= 0 {
		return 0, false
	}
	return id, true
}

func NormalizeMode(mode string) string {
	switch strings.ToLower(strings.TrimSpace(mode)) {
	case ModeOff, ModeHeat, ModeAuto:
		return strings.ToLower(strings.TrimSpace(mode))
	default:
		return ModeAuto
	}
}
