package rules

import "testing"

func TestThermostatDecision(t *testing.T) {
	tests := []struct {
		name       string
		temp       float64
		setpoint   float64
		hyst       float64
		relayOn    bool
		wantPay    string
		wantChange bool
	}{
		{"cold off -> on", 20, 26, 0.5, false, "1", true},
		{"cold already on", 20, 26, 0.5, true, "1", false},
		{"hot on -> off", 27, 26, 0.5, true, "0", true},
		{"hot already off", 27, 26, 0.5, false, "0", false},
		{"inside band keep on", 26.2, 26, 0.5, true, "1", false},
		{"inside band keep off", 25.8, 26, 0.5, false, "0", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pay, change := ThermostatDecision(tt.temp, tt.setpoint, tt.hyst, tt.relayOn)
			if pay != tt.wantPay || change != tt.wantChange {
				t.Fatalf("got %q/%v want %q/%v", pay, change, tt.wantPay, tt.wantChange)
			}
		})
	}
}

func TestThermostatWithMode(t *testing.T) {
	pay, change := ThermostatWithMode(ModeOff, 20, 26, 0.5, true)
	if pay != "0" || !change {
		t.Fatalf("off: %s %v", pay, change)
	}
	pay, change = ThermostatWithMode(ModeHeat, 30, 26, 0.5, false)
	if pay != "1" || !change {
		t.Fatalf("heat: %s %v", pay, change)
	}
	pay, change = ThermostatWithMode(ModeAuto, 20, 26, 0.5, false)
	if pay != "1" || !change {
		t.Fatalf("auto: %s %v", pay, change)
	}
}

