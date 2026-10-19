package floor

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/bartwork/home/internal/repository"
)

type DeviceLister interface {
	List(ctx context.Context) ([]repository.Device, error)
}

type RuleStore interface {
	List(ctx context.Context) ([]repository.Rule, error)
	Create(ctx context.Context, in repository.WriteRule) (repository.Rule, error)
}

type Reloader interface {
	Reload(ctx context.Context) error
}

// EnsureRules создаёт thermostat-правила для активных floor без покрытия
// (meta.floorDeviceId или relay в logic.thermostat).
func EnsureRules(ctx context.Context, devices DeviceLister, store RuleStore, reload Reloader) (created int, err error) {
	devs, err := devices.List(ctx)
	if err != nil {
		return 0, err
	}
	existing, err := store.List(ctx)
	if err != nil {
		return 0, err
	}
	coveredID := map[int64]struct{}{}
	coveredRelay := map[string]struct{}{}
	for _, r := range existing {
		if id, ok := floorDeviceID(r.GraphJSON); ok {
			coveredID[id] = struct{}{}
		}
		for relay := range relaysInGraph(r.GraphJSON) {
			coveredRelay[relay] = struct{}{}
		}
	}

	for _, d := range devs {
		if !d.Active || d.DeviceType != "floor" {
			continue
		}
		relay := strings.TrimSpace(d.MqttTopic)
		sensor := strings.TrimSpace(d.PowerTopic)
		if relay == "" || sensor == "" {
			continue
		}
		if _, ok := coveredID[d.ID]; ok {
			continue
		}
		if _, ok := coveredRelay[relay]; ok {
			continue
		}
		raw, err := ThermostatGraphJSON(d.ID, relay, sensor)
		if err != nil {
			return created, err
		}
		if _, err := store.Create(ctx, repository.WriteRule{
			Name: fmt.Sprintf("floor: %s", d.Name),
			Enabled: true, Mode: "single", CooldownMs: 0, GraphJSON: string(raw),
		}); err != nil {
			return created, err
		}
		created++
		coveredID[d.ID] = struct{}{}
		coveredRelay[relay] = struct{}{}
	}
	if created > 0 && reload != nil {
		_ = reload.Reload(ctx)
	}
	return created, nil
}

func floorDeviceID(graphJSON string) (int64, bool) {
	var g struct {
		Meta *struct {
			FloorDeviceID int64 `json:"floorDeviceId"`
		} `json:"meta"`
	}
	if err := json.Unmarshal([]byte(graphJSON), &g); err != nil || g.Meta == nil {
		return 0, false
	}
	if g.Meta.FloorDeviceID <= 0 {
		return 0, false
	}
	return g.Meta.FloorDeviceID, true
}

func relaysInGraph(graphJSON string) map[string]struct{} {
	out := map[string]struct{}{}
	var g struct {
		Nodes []struct {
			Type string          `json:"type"`
			Data json.RawMessage `json:"data"`
		} `json:"nodes"`
	}
	if err := json.Unmarshal([]byte(graphJSON), &g); err != nil {
		return out
	}
	for _, n := range g.Nodes {
		if n.Type != "logic.thermostat" {
			continue
		}
		var d struct {
			Relay string `json:"relay"`
		}
		if err := json.Unmarshal(n.Data, &d); err != nil {
			continue
		}
		if r := strings.TrimSpace(d.Relay); r != "" {
			out[r] = struct{}{}
		}
	}
	return out
}
