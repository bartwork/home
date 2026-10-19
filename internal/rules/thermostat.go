package rules

import "strings"

const (
	ModeOff  = "off"
	ModeHeat = "heat"
	ModeAuto = "auto"
)

// ThermostatDecision computes relay target for heating with hysteresis in auto mode.
func ThermostatDecision(temp, setpoint, hysteresis float64, relayOn bool) (payload string, change bool) {
	if temp < setpoint-hysteresis {
		if !relayOn {
			return "1", true
		}
		return "1", false
	}
	if temp > setpoint+hysteresis {
		if relayOn {
			return "0", true
		}
		return "0", false
	}
	if relayOn {
		return "1", false
	}
	return "0", false
}

// ThermostatWithMode applies off/heat/auto on top of hysteresis.
func ThermostatWithMode(mode string, temp, setpoint, hysteresis float64, relayOn bool) (payload string, change bool) {
	switch strings.ToLower(strings.TrimSpace(mode)) {
	case ModeOff:
		if relayOn {
			return "0", true
		}
		return "0", false
	case ModeHeat:
		if !relayOn {
			return "1", true
		}
		return "1", false
	default: // auto / empty
		return ThermostatDecision(temp, setpoint, hysteresis, relayOn)
	}
}

func relayIsOn(payload string) bool {
	switch payload {
	case "1", "true", "on", "ON", "True":
		return true
	default:
		return false
	}
}

func normalizeFloorMode(mode string) string {
	switch strings.ToLower(strings.TrimSpace(mode)) {
	case ModeOff, ModeHeat, ModeAuto:
		return strings.ToLower(strings.TrimSpace(mode))
	default:
		return ModeAuto
	}
}
