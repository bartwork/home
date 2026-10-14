package realtime

import "strings"

// CommandTopic — WB write-топик: state → …/on.
func CommandTopic(stateTopic string) string {
	return strings.TrimSpace(stateTopic) + "/on"
}
