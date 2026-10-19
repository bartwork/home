package rules

import (
	"fmt"
	"strings"
)

func ValidateGraph(g Graph) error {
	if len(g.Nodes) == 0 {
		return fmt.Errorf("graph has no nodes")
	}
	ids := make(map[string]struct{}, len(g.Nodes))
	hasTrigger := false
	for _, n := range g.Nodes {
		id := strings.TrimSpace(n.ID)
		if id == "" {
			return fmt.Errorf("node with empty id")
		}
		if _, ok := ids[id]; ok {
			return fmt.Errorf("duplicate node id %q", id)
		}
		ids[id] = struct{}{}
		if err := validateNode(n); err != nil {
			return fmt.Errorf("node %s: %w", id, err)
		}
		switch n.Type {
		case NodeTriggerMQTT, NodeTriggerCron, NodeTriggerManual:
			hasTrigger = true
		}
	}
	if !hasTrigger {
		return fmt.Errorf("graph needs at least one trigger")
	}
	for _, e := range g.Edges {
		if strings.TrimSpace(e.Source) == "" || strings.TrimSpace(e.Target) == "" {
			return fmt.Errorf("edge with empty source/target")
		}
		if _, ok := ids[e.Source]; !ok {
			return fmt.Errorf("edge source %q missing", e.Source)
		}
		if _, ok := ids[e.Target]; !ok {
			return fmt.Errorf("edge target %q missing", e.Target)
		}
	}
	return nil
}

func validateNode(n Node) error {
	switch n.Type {
	case NodeTriggerMQTT:
		d, err := decodeData[TriggerMQTTData](n.Data)
		if err != nil {
			return err
		}
		if strings.TrimSpace(d.Topic) == "" {
			return fmt.Errorf("trigger.mqtt requires topic")
		}
		on := strings.ToLower(strings.TrimSpace(d.On))
		if on != "" && on != "change" && on != "any" {
			return fmt.Errorf("trigger.mqtt.on must be change|any")
		}
	case NodeTriggerCron:
		d, err := decodeData[TriggerCronData](n.Data)
		if err != nil {
			return err
		}
		if strings.TrimSpace(d.Expr) == "" {
			return fmt.Errorf("trigger.cron requires expr")
		}
	case NodeTriggerManual:
	case NodeConditionCompare:
		d, err := decodeData[CompareData](n.Data)
		if err != nil {
			return err
		}
		if err := validateValueRef(d.Left, "left"); err != nil {
			return err
		}
		if err := validateValueRef(d.Right, "right"); err != nil {
			return err
		}
		if !validOp(d.Op) {
			return fmt.Errorf("unsupported op %q", d.Op)
		}
	case NodeConditionAnd, NodeConditionOr:
	case NodeActionMQTT:
		d, err := decodeData[ActionMQTTData](n.Data)
		if err != nil {
			return err
		}
		if strings.TrimSpace(d.Topic) == "" {
			return fmt.Errorf("action.mqtt requires topic")
		}
	case NodeActionDelay:
		d, err := decodeData[DelayData](n.Data)
		if err != nil {
			return err
		}
		if d.Ms < 0 {
			return fmt.Errorf("action.delay.ms must be >= 0")
		}
	case NodeActionSetVar:
		d, err := decodeData[SetVarData](n.Data)
		if err != nil {
			return err
		}
		if strings.TrimSpace(d.Name) == "" {
			return fmt.Errorf("action.set_var requires name")
		}
	case NodeLogicThermostat:
		d, err := decodeData[ThermostatData](n.Data)
		if err != nil {
			return err
		}
		if err := validateValueRef(d.Temp, "temp"); err != nil {
			return err
		}
		if err := validateValueRef(d.Setpoint, "setpoint"); err != nil {
			return err
		}
		if strings.TrimSpace(d.Relay) == "" {
			return fmt.Errorf("logic.thermostat requires relay")
		}
		if d.Hysteresis < 0 {
			return fmt.Errorf("hysteresis must be >= 0")
		}
	default:
		return fmt.Errorf("unknown node type %q", n.Type)
	}
	return nil
}

func validateValueRef(v ValueRef, label string) error {
	hasTopic := strings.TrimSpace(v.Topic) != ""
	hasVar := strings.TrimSpace(v.Var) != ""
	if hasTopic && hasVar {
		return fmt.Errorf("%s: use one of topic|var|const", label)
	}
	return nil
}

func validOp(op string) bool {
	switch strings.ToLower(strings.TrimSpace(op)) {
	case "eq", "ne", "lt", "lte", "gt", "gte":
		return true
	default:
		return false
	}
}

// ValidateGraphJSON parses and validates raw graph JSON.
func ValidateGraphJSON(raw string) error {
	g, err := parseGraph(raw)
	if err != nil {
		return err
	}
	return ValidateGraph(g)
}
