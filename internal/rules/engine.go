package rules

import (
	"context"
	"sync"
	"time"

	"github.com/bartwork/home/internal/discovery"
	"github.com/bartwork/home/internal/repository"
	"github.com/robfig/cron/v3"
	"github.com/rs/zerolog"
)

const (
	selfWriteTTL  = time.Second
	defaultCronTZ = "Europe/Moscow"
)

type Publisher interface {
	Publish(topic, payload string) error
}

type topicHit struct {
	rule *compiledRule
	trig mqttTrig
}

type Engine struct {
	log   zerolog.Logger
	repo  repository.RuleStore
	pub   Publisher
	state *topicState

	mu         sync.RWMutex
	rules      map[int64]*compiledRule
	topicIndex map[string][]topicHit

	cronMu sync.Mutex
	crons  []*cron.Cron

	runMu     sync.Mutex
	running   map[int64]context.CancelFunc
	lastRun   map[int64]time.Time
	selfWrite map[string]selfWrite

	queueMu sync.Mutex
	queued  map[int64]bool
}

type selfWrite struct {
	payload string
	until   time.Time
}

func NewEngine(log zerolog.Logger, repo repository.RuleStore, pub Publisher) *Engine {
	return &Engine{
		log:        log.With().Str("component", "rules").Logger(),
		repo:       repo,
		pub:        pub,
		state:      newTopicState(),
		rules:      make(map[int64]*compiledRule),
		topicIndex: make(map[string][]topicHit),
		running:    make(map[int64]context.CancelFunc),
		lastRun:    make(map[int64]time.Time),
		selfWrite:  make(map[string]selfWrite),
		queued:     make(map[int64]bool),
	}
}

func (e *Engine) Start(ctx context.Context) error { return e.Reload(ctx) }

func (e *Engine) Stop() {
	e.stopCrons()
	e.runMu.Lock()
	for id, cancel := range e.running {
		cancel()
		delete(e.running, id)
	}
	e.runMu.Unlock()
}

func (e *Engine) StateGet(topic string) string { return e.state.Get(topic) }

func (e *Engine) Reload(ctx context.Context) error {
	rows, err := e.repo.List(ctx)
	if err != nil {
		return err
	}
	next := make(map[int64]*compiledRule, len(rows))
	for _, row := range rows {
		cr, err := compileRule(row.ID, row.Name, row.Enabled, row.Mode, row.CooldownMs, row.GraphJSON)
		if err != nil {
			e.log.Error().Err(err).Int64("ruleId", row.ID).Str("name", row.Name).Msg("skip invalid rule")
			continue
		}
		cp := cr
		next[row.ID] = &cp
	}
	index := make(map[string][]topicHit)
	for _, rule := range next {
		if !rule.Enabled {
			continue
		}
		for topic, trigs := range rule.mqttByTopic {
			for _, t := range trigs {
				index[topic] = append(index[topic], topicHit{rule: rule, trig: t})
			}
		}
	}

	e.mu.Lock()
	e.rules = next
	e.topicIndex = index
	e.mu.Unlock()

	e.rebuildCron()
	e.log.Info().Int("rules", len(next)).Msg("rules reloaded")
	return nil
}

func (e *Engine) Observe(topic, payload string) {
	parsed, ok := discovery.ParseTopic(topic)
	if !ok || parsed.Kind != discovery.KindState {
		return
	}
	stateTopic := parsed.StateTopic
	_, changed := e.state.Set(stateTopic, payload)
	if e.isSelfEcho(stateTopic, payload) {
		return
	}

	e.mu.RLock()
	hits := e.topicIndex[stateTopic]
	candidates := make([]scheduled, 0, len(hits))
	for _, h := range hits {
		if h.rule == nil || !h.rule.Enabled {
			continue
		}
		if h.trig.on == "change" && !changed {
			continue
		}
		candidates = append(candidates, scheduled{rule: h.rule, triggerID: h.trig.nodeID})
	}
	e.mu.RUnlock()

	for _, c := range candidates {
		e.scheduleRun(c.rule, c.triggerID)
	}
}

func (e *Engine) RunManual(ctx context.Context, ruleID int64) error {
	e.mu.RLock()
	rule := e.rules[ruleID]
	e.mu.RUnlock()
	if rule == nil {
		return errRuleNotLoaded
	}
	if !rule.Enabled {
		return errRuleDisabled
	}
	var triggerID string
	for id, n := range rule.nodes {
		if n.Type == NodeTriggerManual {
			triggerID = id
			break
		}
	}
	if triggerID == "" {
		return errNoManualTrigger
	}
	e.scheduleRun(rule, triggerID)
	return nil
}

// SyncThermostats runs every logic.thermostat once against current topic cache (startup).
func (e *Engine) SyncThermostats() {
	type job struct {
		rule *compiledRule
		node Node
	}
	e.mu.RLock()
	var jobs []job
	for _, rule := range e.rules {
		if !rule.Enabled {
			continue
		}
		for _, n := range rule.nodes {
			if n.Type == NodeLogicThermostat {
				jobs = append(jobs, job{rule: rule, node: n})
			}
		}
	}
	e.mu.RUnlock()

	for _, j := range jobs {
		rc := &runContext{rule: j.rule, vars: map[string]string{}}
		if err := e.execThermostat(rc, j.node); err != nil {
			e.log.Debug().Err(err).Int64("ruleId", j.rule.ID).Msg("sync thermostat")
		}
	}
}
