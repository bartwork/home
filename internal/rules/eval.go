package rules

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/bartwork/home/internal/realtime"
)

type runContext struct {
	rule   *compiledRule
	vars   map[string]string
	cancel <-chan struct{}
}

func (e *Engine) runFrom(ctx context.Context, rule *compiledRule, triggerID string) error {
	if rule == nil {
		return nil
	}
	rc := &runContext{
		rule:   rule,
		vars:   map[string]string{},
		cancel: ctx.Done(),
	}
	return e.execNode(ctx, rc, triggerID)
}

func (e *Engine) execNode(ctx context.Context, rc *runContext, nodeID string) error {
	select {
	case <-ctx.Done():
		return ctx.Err()
	default:
	}
	n, ok := rc.rule.nodes[nodeID]
	if !ok {
		return fmt.Errorf("missing node %s", nodeID)
	}

	switch n.Type {
	case NodeTriggerMQTT, NodeTriggerCron, NodeTriggerManual:
		return e.follow(ctx, rc, nodeID, "")
	case NodeConditionCompare:
		d, err := decodeData[CompareData](n.Data)
		if err != nil {
			return err
		}
		left := e.resolve(d.Left, rc.vars)
		right := e.resolve(d.Right, rc.vars)
		ok := compareValues(d.Op, left, right)
		handle := HandleFalse
		if ok {
			handle = HandleTrue
		}
		return e.follow(ctx, rc, nodeID, handle)
	case NodeConditionAnd, NodeConditionOr:
		ok, err := e.evalLogicGate(rc, nodeID, n.Type == NodeConditionAnd)
		if err != nil {
			return err
		}
		handle := HandleFalse
		if ok {
			handle = HandleTrue
		}
		return e.follow(ctx, rc, nodeID, handle)
	case NodeActionMQTT:
		d, err := decodeData[ActionMQTTData](n.Data)
		if err != nil {
			return err
		}
		if err := e.publishAction(d.Topic, d.Payload, d.Command); err != nil {
			return err
		}
		return e.follow(ctx, rc, nodeID, "")
	case NodeActionDelay:
		d, err := decodeData[DelayData](n.Data)
		if err != nil {
			return err
		}
		if d.Ms > 0 {
			t := time.NewTimer(time.Duration(d.Ms) * time.Millisecond)
			select {
			case <-ctx.Done():
				t.Stop()
				return ctx.Err()
			case <-t.C:
			}
		}
		return e.follow(ctx, rc, nodeID, "")
	case NodeActionSetVar:
		d, err := decodeData[SetVarData](n.Data)
		if err != nil {
			return err
		}
		rc.vars[strings.TrimSpace(d.Name)] = e.resolve(d.Value, rc.vars)
		return e.follow(ctx, rc, nodeID, "")
	case NodeLogicThermostat:
		if err := e.execThermostat(rc, n); err != nil {
			return err
		}
		return e.follow(ctx, rc, nodeID, "")
	default:
		return fmt.Errorf("unsupported node type %s", n.Type)
	}
}

func (e *Engine) follow(ctx context.Context, rc *runContext, sourceID, handle string) error {
	for _, edge := range rc.rule.out[sourceID] {
		sh := strings.TrimSpace(edge.SourceHandle)
		if handle != "" && sh != "" && sh != handle {
			continue
		}
		if handle == "" && (sh == HandleTrue || sh == HandleFalse) {
			// untyped follow shouldn't take condition branches
			continue
		}
		if err := e.execNode(ctx, rc, edge.Target); err != nil {
			return err
		}
	}
	return nil
}

func (e *Engine) evalLogicGate(rc *runContext, nodeID string, and bool) (bool, error) {
	ins := rc.rule.in[nodeID]
	if len(ins) == 0 {
		return and, nil // empty AND = true, empty OR = false → use and flag: AND empty true, OR empty false
	}
	if and {
		for _, edge := range ins {
			ok, err := e.evalAsBool(rc, edge.Source)
			if err != nil {
				return false, err
			}
			if !ok {
				return false, nil
			}
		}
		return true, nil
	}
	for _, edge := range ins {
		ok, err := e.evalAsBool(rc, edge.Source)
		if err != nil {
			return false, err
		}
		if ok {
			return true, nil
		}
	}
	return false, nil
}

func (e *Engine) evalAsBool(rc *runContext, nodeID string) (bool, error) {
	n, ok := rc.rule.nodes[nodeID]
	if !ok {
		return false, fmt.Errorf("missing node %s", nodeID)
	}
	switch n.Type {
	case NodeTriggerMQTT, NodeTriggerCron, NodeTriggerManual:
		return true, nil
	case NodeConditionCompare:
		d, err := decodeData[CompareData](n.Data)
		if err != nil {
			return false, err
		}
		return compareValues(d.Op, e.resolve(d.Left, rc.vars), e.resolve(d.Right, rc.vars)), nil
	case NodeConditionAnd, NodeConditionOr:
		return e.evalLogicGate(rc, nodeID, n.Type == NodeConditionAnd)
	default:
		return true, nil
	}
}

func (e *Engine) execThermostat(rc *runContext, n Node) error {
	d, err := decodeData[ThermostatData](n.Data)
	if err != nil {
		return err
	}
	mode := ModeAuto
	if d.Mode.Topic != "" || d.Mode.Var != "" || d.Mode.Const != "" {
		mode = normalizeFloorMode(e.resolve(d.Mode, rc.vars))
	}
	temp, okT := parseFloat(e.resolve(d.Temp, rc.vars))
	setpoint, okS := parseFloat(e.resolve(d.Setpoint, rc.vars))
	if !okS {
		setpoint = 26
	}
	if mode == ModeAuto && !okT {
		return nil // wait for sensor
	}
	hyst := d.Hysteresis
	if hyst == 0 {
		hyst = 0.5
	}
	relayTopic := strings.TrimSpace(d.Relay)
	current := e.state.Get(relayTopic)
	payload, change := ThermostatWithMode(mode, temp, setpoint, hyst, relayIsOn(current))
	if !change {
		return nil
	}
	return e.publishAction(relayTopic, payload, d.Command)
}

func (e *Engine) publishAction(topic, payload string, command bool) error {
	topic = strings.TrimSpace(topic)
	if topic == "" {
		return fmt.Errorf("empty publish topic")
	}
	pubTopic := topic
	if command {
		pubTopic = realtime.CommandTopic(topic)
	}
	e.noteSelfWrite(topic, payload)
	if e.pub == nil {
		return fmt.Errorf("publisher not configured")
	}
	return e.pub.Publish(pubTopic, payload)
}
