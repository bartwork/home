package rules

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/bartwork/home/internal/repository"
	"github.com/rs/zerolog"
)

type memPub struct {
	mu   sync.Mutex
	msgs [][2]string
}

func (p *memPub) Publish(topic, payload string) error {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.msgs = append(p.msgs, [2]string{topic, payload})
	return nil
}

func (p *memPub) last() (string, string, bool) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if len(p.msgs) == 0 {
		return "", "", false
	}
	m := p.msgs[len(p.msgs)-1]
	return m[0], m[1], true
}

type memRepo struct {
	mu    sync.Mutex
	rules []repository.Rule
}

func (r *memRepo) List(context.Context) ([]repository.Rule, error) {
	r.mu.Lock()
	defer r.mu.Unlock()
	out := make([]repository.Rule, len(r.rules))
	copy(out, r.rules)
	return out, nil
}
func (r *memRepo) Get(context.Context, int64) (repository.Rule, error) {
	return repository.Rule{}, nil
}
func (r *memRepo) Create(context.Context, repository.WriteRule) (repository.Rule, error) {
	return repository.Rule{}, nil
}
func (r *memRepo) Update(context.Context, int64, repository.WriteRule) (repository.Rule, error) {
	return repository.Rule{}, nil
}
func (r *memRepo) SetEnabled(context.Context, int64, bool) (repository.Rule, error) {
	return repository.Rule{}, nil
}
func (r *memRepo) Delete(context.Context, int64) error { return nil }

func TestGraphWalkCompareTrue(t *testing.T) {
	graph := `{
		"nodes":[
			{"id":"t","type":"trigger.manual","position":{"x":0,"y":0},"data":{}},
			{"id":"c","type":"condition.compare","position":{"x":0,"y":0},
				"data":{"left":{"const":"1"},"op":"eq","right":{"const":"1"}}},
			{"id":"a","type":"action.mqtt","position":{"x":0,"y":0},
				"data":{"topic":"/devices/d/controls/K1","payload":"1","command":true}}
		],
		"edges":[
			{"id":"e1","source":"t","target":"c"},
			{"id":"e2","source":"c","target":"a","sourceHandle":"true"}
		]
	}`
	pub := &memPub{}
	repo := &memRepo{rules: []repository.Rule{{
		ID: 1, Name: "t", Enabled: true, Mode: ModeSingle, GraphJSON: graph,
	}}}
	eng := NewEngine(zerolog.Nop(), repo, pub)
	if err := eng.Reload(context.Background()); err != nil {
		t.Fatal(err)
	}
	if err := eng.RunManual(context.Background(), 1); err != nil {
		t.Fatal(err)
	}
	waitFor(t, func() bool {
		_, _, ok := pub.last()
		return ok
	})
	topic, payload, ok := pub.last()
	if !ok || topic != "/devices/d/controls/K1/on" || payload != "1" {
		t.Fatalf("got %s=%s", topic, payload)
	}
}

func TestModeSingleSkipsOverlap(t *testing.T) {
	graph := `{
		"nodes":[
			{"id":"t","type":"trigger.manual","position":{"x":0,"y":0},"data":{}},
			{"id":"d","type":"action.delay","position":{"x":0,"y":0},"data":{"ms":80}},
			{"id":"a","type":"action.mqtt","position":{"x":0,"y":0},
				"data":{"topic":"/devices/d/controls/K1","payload":"1","command":true}}
		],
		"edges":[
			{"id":"e1","source":"t","target":"d"},
			{"id":"e2","source":"d","target":"a"}
		]
	}`
	pub := &memPub{}
	repo := &memRepo{rules: []repository.Rule{{
		ID: 1, Name: "t", Enabled: true, Mode: ModeSingle, GraphJSON: graph,
	}}}
	eng := NewEngine(zerolog.Nop(), repo, pub)
	_ = eng.Reload(context.Background())
	_ = eng.RunManual(context.Background(), 1)
	_ = eng.RunManual(context.Background(), 1)
	waitFor(t, func() bool {
		pub.mu.Lock()
		n := len(pub.msgs)
		pub.mu.Unlock()
		return n >= 1
	})
	time.Sleep(120 * time.Millisecond)
	pub.mu.Lock()
	n := len(pub.msgs)
	pub.mu.Unlock()
	if n != 1 {
		t.Fatalf("expected 1 publish, got %d", n)
	}
}

func TestCooldown(t *testing.T) {
	graph := `{
		"nodes":[
			{"id":"t","type":"trigger.manual","position":{"x":0,"y":0},"data":{}},
			{"id":"a","type":"action.mqtt","position":{"x":0,"y":0},
				"data":{"topic":"/devices/d/controls/K1","payload":"1","command":false}}
		],
		"edges":[{"id":"e1","source":"t","target":"a"}]
	}`
	pub := &memPub{}
	repo := &memRepo{rules: []repository.Rule{{
		ID: 1, Name: "t", Enabled: true, Mode: ModeSingle, CooldownMs: 500, GraphJSON: graph,
	}}}
	eng := NewEngine(zerolog.Nop(), repo, pub)
	_ = eng.Reload(context.Background())
	_ = eng.RunManual(context.Background(), 1)
	waitFor(t, func() bool { _, _, ok := pub.last(); return ok })
	_ = eng.RunManual(context.Background(), 1)
	time.Sleep(30 * time.Millisecond)
	pub.mu.Lock()
	n := len(pub.msgs)
	pub.mu.Unlock()
	if n != 1 {
		t.Fatalf("cooldown failed: %d msgs", n)
	}
}

func TestMQTTThermostat(t *testing.T) {
	temp := "/devices/wb-m1w2_83/controls/External Sensor 1"
	relay := "/devices/wb-mr6cv3_225/controls/K1"
	graph := `{
		"nodes":[
			{"id":"t","type":"trigger.mqtt","position":{"x":0,"y":0},
				"data":{"topic":"` + temp + `","on":"change"}},
			{"id":"th","type":"logic.thermostat","position":{"x":0,"y":0},
				"data":{"temp":{"topic":"` + temp + `"},"setpoint":{"const":"26"},"hysteresis":0.5,
					"relay":"` + relay + `","command":true}}
		],
		"edges":[{"id":"e1","source":"t","target":"th"}]
	}`
	pub := &memPub{}
	repo := &memRepo{rules: []repository.Rule{{
		ID: 1, Name: "floor", Enabled: true, Mode: ModeSingle, GraphJSON: graph,
	}}}
	eng := NewEngine(zerolog.Nop(), repo, pub)
	_ = eng.Reload(context.Background())
	eng.state.Set(relay, "0")
	eng.Observe(temp, "20")
	waitFor(t, func() bool {
		topic, payload, ok := pub.last()
		return ok && topic == relay+"/on" && payload == "1"
	})
}

func waitFor(t *testing.T, ok func() bool) {
	t.Helper()
	deadline := time.Now().Add(2 * time.Second)
	for time.Now().Before(deadline) {
		if ok() {
			return
		}
		time.Sleep(5 * time.Millisecond)
	}
	t.Fatal("timeout")
}
