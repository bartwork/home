package discovery

import (
	"encoding/json"
	"strings"

	"github.com/bartwork/home/internal/air"
	"github.com/bartwork/home/internal/layout"
)

// ParsedTopic — разобранный Wirenboard-топик.
type ParsedTopic struct {
	Kind      TopicKind
	DeviceID  string
	Control   string
	MetaField string // type|title|units|… для /meta/<field>
	StateTopic string
}

type TopicKind int

const (
	KindState TopicKind = iota
	KindControlMetaJSON  // …/controls/x/meta  (JSON body)
	KindControlMetaField // …/controls/x/meta/type
)

// ParseTopic разбирает /devices/{id}/controls/{control}[/meta[/field]].
func ParseTopic(topic string) (ParsedTopic, bool) {
	topic = strings.TrimSpace(topic)
	parts := strings.Split(strings.Trim(topic, "/"), "/")
	// devices / id / controls / control [/ meta [/ field ]]
	if len(parts) < 4 || parts[0] != "devices" || parts[2] != "controls" {
		return ParsedTopic{}, false
	}
	deviceID := parts[1]
	control := parts[3]
	if deviceID == "" || control == "" {
		return ParsedTopic{}, false
	}
	state := "/devices/" + deviceID + "/controls/" + control

	if len(parts) == 4 {
		return ParsedTopic{Kind: KindState, DeviceID: deviceID, Control: control, StateTopic: state}, true
	}
	if parts[4] != "meta" {
		return ParsedTopic{}, false
	}
	if len(parts) == 5 {
		return ParsedTopic{Kind: KindControlMetaJSON, DeviceID: deviceID, Control: control, StateTopic: state}, true
	}
	if len(parts) == 6 {
		return ParsedTopic{
			Kind: KindControlMetaField, DeviceID: deviceID, Control: control,
			MetaField: parts[5], StateTopic: state,
		}, true
	}
	return ParsedTopic{}, false
}

// ClassifyWBType maps Wirenboard meta type → our device_type.
func ClassifyWBType(wbType string) string {
	switch strings.ToLower(strings.TrimSpace(wbType)) {
	case "switch", "pushbutton", "alarm":
		return "switch"
	case "range":
		return "range"
	case "value", "temperature", "rel_humidity", "atmospheric_pressure",
		"precipitation_rate", "wind_speed", "power", "voltage", "current",
		"energy", "water_meter", "heat_meter", "lux", "sound_level",
		"concentration", "pressure":
		return "sensor"
	case "text", "rgb", "actuator":
		return "raw"
	default:
		if wbType == "" {
			return ""
		}
		return "raw"
	}
}

// SuggestDeviceType учитывает устройство WB (ONOKOM / WB-LED / тёплый пол / MSW) поверх meta-типа.
// ONOKOM Active → climate; WB-LED RGB Strip → led; реле ТП → floor; MSW Temperature → air; sibling-каналы → raw.
func SuggestDeviceType(deviceID, control, wbType string) string {
	if isOnokomAC(deviceID) {
		if strings.EqualFold(strings.TrimSpace(control), "Active") {
			return "climate"
		}
		return "raw"
	}
	if isWBLed(deviceID) {
		c := strings.TrimSpace(control)
		if strings.EqualFold(c, "RGB Strip") {
			return "led"
		}
		if isWBLedRGBStripSibling(c) {
			return "raw"
		}
	}
	if air.IsMSWDevice(deviceID) {
		c := strings.TrimSpace(control)
		if strings.EqualFold(c, air.ControlTemperature) {
			return "air"
		}
		return "raw"
	}
	if layout.IsFloorRelay(deviceID, control) {
		return "floor"
	}
	if layout.IsFloorTempSensor(deviceID, control) {
		return "raw"
	}
	return ClassifyWBType(wbType)
}

func isOnokomAC(deviceID string) bool {
	id := strings.ToUpper(strings.TrimSpace(deviceID))
	return strings.HasPrefix(id, "ONOKOM-AIR-")
}

func isWBLed(deviceID string) bool {
	id := strings.ToLower(strings.TrimSpace(deviceID))
	return strings.HasPrefix(id, "wb-led")
}

// Sibling-контролы RGB-режима WB-LED (яркость/HSV/палитра/белый канал).
func isWBLedRGBStripSibling(control string) bool {
	c := strings.TrimSpace(control)
	switch {
	case strings.HasPrefix(c, "RGB Strip "):
		return true
	case strings.EqualFold(c, "RGB Palette"):
		return true
	case strings.HasPrefix(c, "Hue Changing"):
		return true
	case strings.EqualFold(c, "Channel 4"), strings.EqualFold(c, "Channel 4 Brightness"):
		return true
	default:
		return false
	}
}

type MetaPatch struct {
	Type  string
	Title string
	Units string
}

// ParseMetaPayload читает JSON meta или одиночное текстовое поле.
func ParseMetaPayload(kind TopicKind, field, payload string) MetaPatch {
	payload = strings.TrimSpace(payload)
	if kind == KindControlMetaField {
		switch field {
		case "type":
			return MetaPatch{Type: payload}
		case "title", "name":
			return MetaPatch{Title: stripQuotes(payload)}
		case "units", "unit":
			return MetaPatch{Units: stripQuotes(payload)}
		default:
			return MetaPatch{}
		}
	}
	if payload == "" {
		return MetaPatch{}
	}
	var m map[string]any
	if err := json.Unmarshal([]byte(payload), &m); err != nil {
		// иногда type приходит plain text на .../meta
		if !strings.Contains(payload, "{") {
			return MetaPatch{Type: payload}
		}
		return MetaPatch{}
	}
	var out MetaPatch
	if v, ok := m["type"].(string); ok {
		out.Type = v
	}
	if v, ok := m["title"].(string); ok {
		out.Title = v
	} else if v, ok := m["name"].(string); ok {
		out.Title = v
	}
	if v, ok := m["units"].(string); ok {
		out.Units = v
	} else if v, ok := m["unit"].(string); ok {
		out.Units = v
	}
	return out
}

func stripQuotes(s string) string {
	return strings.Trim(s, `"'`)
}

func DefaultTitle(deviceID, control string) string {
	if control == "" {
		return deviceID
	}
	return deviceID + " · " + control
}
