package air

import (
	"strconv"
	"strings"
)

// Пороги CO₂ (единые с UI airTopics) + гистерезис против мигания LED.
const (
	CO2GoodMax   = 800
	CO2HighMin   = 1200
	CO2GoodLeave = 900  // выше — уже не «норма»
	CO2HighLeave = 1100 // ниже — уже не «проветри»
)

type Level string

const (
	LevelGood Level = "good" // норма → зелёный
	LevelOK   Level = "ok"   // душновато → оба выкл
	LevelHigh Level = "high" // проветри → красный
)

func ParseCO2(payload string) (float64, bool) {
	s := strings.TrimSpace(strings.ReplaceAll(payload, ",", "."))
	if s == "" || s == "—" {
		return 0, false
	}
	n, err := strconv.ParseFloat(s, 64)
	if err != nil {
		return 0, false
	}
	return n, true
}

// NextLevel учитывает предыдущий уровень (гистерезис на границах).
func NextLevel(prev Level, ppm float64) Level {
	switch prev {
	case LevelHigh:
		if ppm >= CO2HighLeave {
			return LevelHigh
		}
	case LevelGood:
		if ppm < CO2GoodLeave {
			return LevelGood
		}
	}
	if ppm >= CO2HighMin {
		return LevelHigh
	}
	if ppm < CO2GoodMax {
		return LevelGood
	}
	return LevelOK
}

// LEDPayloads: red/green state "0"|"1".
func LEDPayloads(level Level) (red, green string) {
	switch level {
	case LevelHigh:
		return "1", "0"
	case LevelGood:
		return "0", "1"
	default:
		return "0", "0"
	}
}
