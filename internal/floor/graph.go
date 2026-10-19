package floor

import "encoding/json"

// ThermostatGraphJSON — React Flow IR для одного тёплого пола.
func ThermostatGraphJSON(deviceID int64, relay, sensor string) ([]byte, error) {
	setpoint := SetpointTopic(deviceID)
	mode := ModeTopic(deviceID)
	g := map[string]any{
		"meta": map[string]any{"floorDeviceId": deviceID},
		"nodes": []map[string]any{
			node("t_temp", "trigger.mqtt", 0, 0, map[string]any{"topic": sensor, "on": "change"}),
			node("t_setpoint", "trigger.mqtt", 0, 100, map[string]any{"topic": setpoint, "on": "change"}),
			node("t_mode", "trigger.mqtt", 0, 200, map[string]any{"topic": mode, "on": "change"}),
			node("thermo", "logic.thermostat", 280, 80, map[string]any{
				"temp": map[string]any{"topic": sensor},
				"setpoint": map[string]any{"topic": setpoint},
				"mode": map[string]any{"topic": mode},
				"hysteresis": DefaultHyst,
				"relay": relay, "command": true,
			}),
		},
		"edges": []map[string]any{
			{"id": "e_temp", "source": "t_temp", "target": "thermo"},
			{"id": "e_sp", "source": "t_setpoint", "target": "thermo"},
			{"id": "e_mode", "source": "t_mode", "target": "thermo"},
		},
	}
	return json.Marshal(g)
}

func node(id, typ string, x, y float64, data map[string]any) map[string]any {
	return map[string]any{
		"id": id, "type": typ,
		"position": map[string]any{"x": x, "y": y},
		"data":     data,
	}
}
