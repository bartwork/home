package rules

import (
	"encoding/json"
	"fmt"
	"strings"
)

const (
	ModeSingle  = "single"
	ModeRestart = "restart"
	ModeQueued  = "queued"

	NodeTriggerMQTT      = "trigger.mqtt"
	NodeTriggerCron      = "trigger.cron"
	NodeTriggerManual    = "trigger.manual"
	NodeConditionCompare = "condition.compare"
	NodeConditionAnd     = "condition.and"
	NodeConditionOr      = "condition.or"
	NodeActionMQTT       = "action.mqtt"
	NodeActionDelay      = "action.delay"
	NodeActionSetVar     = "action.set_var"
	NodeLogicThermostat  = "logic.thermostat"

	HandleTrue  = "true"
	HandleFalse = "false"
)

type GraphMeta struct {
	FloorDeviceID int64 `json:"floorDeviceId,omitempty"`
}

type Graph struct {
	Meta     *GraphMeta `json:"meta,omitempty"`
	Nodes    []Node     `json:"nodes"`
	Edges    []Edge     `json:"edges"`
	Viewport *Viewport  `json:"viewport,omitempty"`
}

type Viewport struct {
	X    float64 `json:"x"`
	Y    float64 `json:"y"`
	Zoom float64 `json:"zoom"`
}

type Node struct {
	ID       string          `json:"id"`
	Type     string          `json:"type"`
	Position Position        `json:"position"`
	Data     json.RawMessage `json:"data"`
}

type Position struct {
	X float64 `json:"x"`
	Y float64 `json:"y"`
}

type Edge struct {
	ID           string `json:"id"`
	Source       string `json:"source"`
	Target       string `json:"target"`
	SourceHandle string `json:"sourceHandle,omitempty"`
	TargetHandle string `json:"targetHandle,omitempty"`
}

// ValueRef resolves to a string from topic cache, run vars, or a constant.
type ValueRef struct {
	Topic string `json:"topic,omitempty"`
	Var   string `json:"var,omitempty"`
	Const string `json:"const,omitempty"`
}

type TriggerMQTTData struct {
	Topic string `json:"topic"`
	On    string `json:"on"` // change | any
}

type TriggerCronData struct {
	Expr     string `json:"expr"`
	Timezone string `json:"timezone"`
}

type CompareData struct {
	Left  ValueRef `json:"left"`
	Op    string   `json:"op"`
	Right ValueRef `json:"right"`
}

type ActionMQTTData struct {
	Topic   string `json:"topic"`
	Payload string `json:"payload"`
	Command bool   `json:"command"`
}

type DelayData struct {
	Ms int64 `json:"ms"`
}

type SetVarData struct {
	Name  string   `json:"name"`
	Value ValueRef `json:"value"`
}

type ThermostatData struct {
	Temp       ValueRef `json:"temp"`
	Setpoint   ValueRef `json:"setpoint"`
	Mode       ValueRef `json:"mode"` // off|heat|auto (optional → auto)
	Hysteresis float64  `json:"hysteresis"`
	Relay      string   `json:"relay"`
	Command    bool     `json:"command"`
}

type mqttTrig struct {
	nodeID string
	on     string // change | any
}

type compiledRule struct {
	ID         int64
	Name       string
	Enabled    bool
	Mode       string
	CooldownMs int64
	Graph      Graph
	nodes      map[string]Node
	out        map[string][]Edge
	in         map[string][]Edge
	mqttByTopic map[string][]mqttTrig
}

func parseGraph(raw string) (Graph, error) {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return Graph{}, fmt.Errorf("empty graph")
	}
	var g Graph
	if err := json.Unmarshal([]byte(raw), &g); err != nil {
		return Graph{}, fmt.Errorf("graph json: %w", err)
	}
	return g, nil
}

func compileRule(id int64, name string, enabled bool, mode string, cooldownMs int64, graphJSON string) (compiledRule, error) {
	g, err := parseGraph(graphJSON)
	if err != nil {
		return compiledRule{}, err
	}
	if err := ValidateGraph(g); err != nil {
		return compiledRule{}, err
	}
	cr := compiledRule{
		ID: id, Name: name, Enabled: enabled, Mode: normalizeRuleMode(mode),
		CooldownMs: cooldownMs, Graph: g,
		nodes:       make(map[string]Node, len(g.Nodes)),
		out:         make(map[string][]Edge),
		in:          make(map[string][]Edge),
		mqttByTopic: make(map[string][]mqttTrig),
	}
	for _, n := range g.Nodes {
		cr.nodes[n.ID] = n
		if n.Type != NodeTriggerMQTT {
			continue
		}
		d, err := decodeData[TriggerMQTTData](n.Data)
		if err != nil {
			continue
		}
		topic := strings.TrimSpace(d.Topic)
		if topic == "" {
			continue
		}
		on := d.On
		if on == "" {
			on = "change"
		}
		cr.mqttByTopic[topic] = append(cr.mqttByTopic[topic], mqttTrig{nodeID: n.ID, on: on})
	}
	for _, e := range g.Edges {
		cr.out[e.Source] = append(cr.out[e.Source], e)
		cr.in[e.Target] = append(cr.in[e.Target], e)
	}
	return cr, nil
}

func normalizeRuleMode(mode string) string {
	switch strings.ToLower(strings.TrimSpace(mode)) {
	case ModeRestart:
		return ModeRestart
	case ModeQueued:
		return ModeQueued
	default:
		return ModeSingle
	}
}

func decodeData[T any](raw json.RawMessage) (T, error) {
	var out T
	if len(raw) == 0 || string(raw) == "null" {
		return out, nil
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return out, err
	}
	return out, nil
}
