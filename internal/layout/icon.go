package layout

import "strings"

// IconFor выводит иконку из явного значения или имени/типа.
func IconFor(explicit, name, deviceType string) string {
	if explicit != "" {
		return explicit
	}
	n := strings.ToLower(name)
	switch {
	case strings.Contains(n, "вытяж"):
		return "fan"
	case strings.Contains(n, "кондиц") || strings.Contains(n, "кондей"):
		return "ac"
	case strings.Contains(n, "тёпл") || strings.Contains(n, "тепл"):
		return "thermo"
	case strings.Contains(n, "розет"):
		return "socket"
	case deviceType == "climate":
		return "ac"
	case deviceType == "led":
		return "light"
	case deviceType == "range":
		return "dimmer"
	case deviceType == "sensor":
		return "sensor"
	case strings.Contains(n, "свет") || strings.Contains(n, "лент") || strings.Contains(n, "бра") || strings.Contains(n, "зеркал"):
		return "light"
	case deviceType == "switch":
		return "switch"
	default:
		return ""
	}
}
