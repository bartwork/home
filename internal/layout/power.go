package layout

import (
	"regexp"
	"strings"
)

var mdm3Channel = regexp.MustCompile(`(?i)^(/devices/wb-mdm3_[^/]+/controls/)Channel\s+(\d+)$`)

// InferPowerTopic для range-диммера WB-MDM3: Channel N → KN.
func InferPowerTopic(dimmerTopic string) string {
	topic := strings.TrimSpace(dimmerTopic)
	if m := mdm3Channel.FindStringSubmatch(topic); m != nil {
		return m[1] + "K" + m[2]
	}
	return ""
}
