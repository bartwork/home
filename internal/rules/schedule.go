package rules

import (
	"context"
	"time"
)

type scheduled struct {
	rule      *compiledRule
	triggerID string
}

func (e *Engine) scheduleRun(rule *compiledRule, triggerID string) {
	if rule == nil || !rule.Enabled {
		return
	}
	now := time.Now()
	e.runMu.Lock()
	if rule.CooldownMs > 0 {
		if last, ok := e.lastRun[rule.ID]; ok && now.Sub(last) < time.Duration(rule.CooldownMs)*time.Millisecond {
			e.runMu.Unlock()
			return
		}
	}
	switch rule.Mode {
	case ModeSingle:
		if _, busy := e.running[rule.ID]; busy {
			e.runMu.Unlock()
			return
		}
	case ModeRestart:
		if cancel, busy := e.running[rule.ID]; busy {
			cancel()
			delete(e.running, rule.ID)
		}
	case ModeQueued:
		if _, busy := e.running[rule.ID]; busy {
			e.queueMu.Lock()
			e.queued[rule.ID] = true
			e.queueMu.Unlock()
			e.runMu.Unlock()
			return
		}
	}
	ctx, cancel := context.WithCancel(context.Background())
	e.running[rule.ID] = cancel
	e.lastRun[rule.ID] = now
	e.runMu.Unlock()

	go e.execute(ctx, rule, triggerID)
}

func (e *Engine) execute(ctx context.Context, rule *compiledRule, triggerID string) {
	defer func() {
		e.runMu.Lock()
		delete(e.running, rule.ID)
		e.runMu.Unlock()

		e.queueMu.Lock()
		again := e.queued[rule.ID]
		delete(e.queued, rule.ID)
		e.queueMu.Unlock()
		if again {
			e.mu.RLock()
			fresh := e.rules[rule.ID]
			e.mu.RUnlock()
			if fresh != nil && fresh.Enabled {
				e.scheduleRun(fresh, triggerID)
			}
		}
	}()

	if err := e.runFrom(ctx, rule, triggerID); err != nil && ctx.Err() == nil {
		e.log.Warn().Err(err).Int64("ruleId", rule.ID).Str("name", rule.Name).Msg("rule run failed")
	}
}

func (e *Engine) noteSelfWrite(stateTopic, payload string) {
	e.runMu.Lock()
	e.selfWrite[stateTopic] = selfWrite{payload: payload, until: time.Now().Add(selfWriteTTL)}
	e.runMu.Unlock()
}

func (e *Engine) isSelfEcho(stateTopic, payload string) bool {
	e.runMu.Lock()
	defer e.runMu.Unlock()
	sw, ok := e.selfWrite[stateTopic]
	if !ok {
		return false
	}
	if time.Now().After(sw.until) {
		delete(e.selfWrite, stateTopic)
		return false
	}
	if sw.payload == payload {
		delete(e.selfWrite, stateTopic)
		return true
	}
	return false
}
