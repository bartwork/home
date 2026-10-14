package layout

import (
	"regexp"
	"strings"
)

var mdm3Channel = regexp.MustCompile(`(?i)^(/devices/wb-mdm3_[^/]+/controls/)Channel\s+(\d+)$`)

// InferPowerTopic выводит топик вкл/выкл для диммера:
//   - WB-MDM3: Channel N → K N
//   - LED/прочее: «… Brightness» → тот же control без суффикса
func InferPowerTopic(dimmerTopic string) string {
	topic := strings.TrimSpace(dimmerTopic)
	if m := mdm3Channel.FindStringSubmatch(topic); m != nil {
		return m[1] + "K" + m[2]
	}
	_, ctrl, ok := SplitWBTopic(topic)
	if !ok {
		return ""
	}
	const suf = " Brightness"
	if !strings.HasSuffix(ctrl, suf) {
		return ""
	}
	return strings.TrimSuffix(topic, suf)
}
