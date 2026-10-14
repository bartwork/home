package layout

import "strings"

// SplitWBTopic разбирает /devices/{id}/controls/{control}.
func SplitWBTopic(topic string) (deviceID, control string, ok bool) {
	parts := strings.Split(strings.Trim(strings.TrimSpace(topic), "/"), "/")
	if len(parts) < 4 || parts[0] != "devices" || parts[2] != "controls" {
		return "", "", false
	}
	if parts[1] == "" || parts[3] == "" {
		return "", "", false
	}
	return parts[1], parts[3], true
}
