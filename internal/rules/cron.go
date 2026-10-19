package rules

import (
	"time"

	"github.com/robfig/cron/v3"
)

func (e *Engine) stopCrons() {
	e.cronMu.Lock()
	defer e.cronMu.Unlock()
	for _, c := range e.crons {
		c.Stop()
	}
	e.crons = nil
}

func (e *Engine) rebuildCron() {
	e.stopCrons()

	type job struct {
		ruleID   int64
		nodeID   string
		expr     string
		timezone string
	}

	e.mu.RLock()
	var jobs []job
	for _, rule := range e.rules {
		if !rule.Enabled {
			continue
		}
		for _, n := range rule.nodes {
			if n.Type != NodeTriggerCron {
				continue
			}
			d, err := decodeData[TriggerCronData](n.Data)
			if err != nil || d.Expr == "" {
				continue
			}
			tz := d.Timezone
			if tz == "" {
				tz = defaultCronTZ
			}
			jobs = append(jobs, job{ruleID: rule.ID, nodeID: n.ID, expr: d.Expr, timezone: tz})
		}
	}
	e.mu.RUnlock()

	if len(jobs) == 0 {
		return
	}

	byTZ := map[string][]job{}
	for _, j := range jobs {
		byTZ[j.timezone] = append(byTZ[j.timezone], j)
	}

	parser := cron.NewParser(cron.Minute | cron.Hour | cron.Dom | cron.Month | cron.Dow | cron.Descriptor)
	var started []*cron.Cron
	for tz, list := range byTZ {
		loc, err := time.LoadLocation(tz)
		if err != nil {
			e.log.Warn().Err(err).Str("tz", tz).Msg("bad timezone; using local")
			loc = time.Local
		}
		c := cron.New(cron.WithLocation(loc), cron.WithParser(parser))
		for _, j := range list {
			j := j
			_, err := c.AddFunc(j.expr, func() {
				e.mu.RLock()
				fresh := e.rules[j.ruleID]
				e.mu.RUnlock()
				if fresh == nil || !fresh.Enabled {
					return
				}
				e.scheduleRun(fresh, j.nodeID)
			})
			if err != nil {
				e.log.Warn().Err(err).Str("expr", j.expr).Int64("ruleId", j.ruleID).Msg("cron add")
			}
		}
		c.Start()
		started = append(started, c)
	}

	e.cronMu.Lock()
	e.crons = started
	e.cronMu.Unlock()
}
