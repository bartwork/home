package air

import (
	"context"
	"strings"
	"sync"

	"github.com/rs/zerolog"

	"github.com/bartwork/home/internal/realtime"
	"github.com/bartwork/home/internal/repository"
)

type Publisher interface {
	Publish(topic, payload string) error
}

type DeviceLister interface {
	List(ctx context.Context) ([]repository.Device, error)
}

type StateReader interface {
	StateGet(topic string) string
}

// Controller — CO₂ → Red/Green LED для устройств типа air (WB-MSW).
type Controller struct {
	log     zerolog.Logger
	pub     Publisher
	devices DeviceLister

	mu      sync.Mutex
	byCO2   map[string]string // co2 topic → Temperature anchor
	level   map[string]Level  // co2 topic → last level
	ledOut  map[string]string // led state topic → last payload
	ignored map[string]struct{}
}

func New(log zerolog.Logger, pub Publisher, devices DeviceLister) *Controller {
	return &Controller{
		log:     log,
		pub:     pub,
		devices: devices,
		byCO2:   map[string]string{},
		level:   map[string]Level{},
		ledOut:  map[string]string{},
		ignored: map[string]struct{}{},
	}
}

func (c *Controller) Observe(topic, payload string) {
	topic = strings.TrimSpace(topic)
	if topic == "" || c.pub == nil {
		return
	}
	if _, ok := ParseMSWCO2(topic); !ok {
		return
	}
	c.mu.Lock()
	if _, skip := c.ignored[topic]; skip {
		c.mu.Unlock()
		return
	}
	c.mu.Unlock()

	anchor, ok := c.lookupAnchor(topic)
	if !ok {
		return
	}
	ppm, ok := ParseCO2(payload)
	if !ok {
		return
	}
	c.apply(topic, anchor, ppm)
}

// ApplyPayload выставляет LED по сырому payload CO₂ (bootstrap / sync).
func (c *Controller) ApplyPayload(co2Topic, payload string) {
	if c.pub == nil {
		return
	}
	anchor, ok := c.lookupAnchor(co2Topic)
	if !ok {
		return
	}
	ppm, ok := ParseCO2(payload)
	if !ok {
		return
	}
	c.apply(co2Topic, anchor, ppm)
}

func (c *Controller) apply(co2Topic, anchor string, ppm float64) {
	c.mu.Lock()
	prev := c.level[co2Topic]
	next := NextLevel(prev, ppm)
	changed := prev != next
	c.level[co2Topic] = next
	c.mu.Unlock()

	if !changed && prev != "" {
		return
	}
	red, green := LEDPayloads(next)
	if err := c.setLED(RedLEDTopic(anchor), red); err != nil {
		c.log.Warn().Err(err).Str("topic", RedLEDTopic(anchor)).Msg("air red led")
	}
	if err := c.setLED(GreenLEDTopic(anchor), green); err != nil {
		c.log.Warn().Err(err).Str("topic", GreenLEDTopic(anchor)).Msg("air green led")
	}
	c.log.Info().
		Str("co2", co2Topic).
		Float64("ppm", ppm).
		Str("level", string(next)).
		Str("red", red).
		Str("green", green).
		Msg("air led")
}

func (c *Controller) setLED(stateTopic, payload string) error {
	stateTopic = strings.TrimSpace(stateTopic)
	if stateTopic == "" {
		return nil
	}
	c.mu.Lock()
	if c.ledOut[stateTopic] == payload {
		c.mu.Unlock()
		return nil
	}
	c.mu.Unlock()

	if err := c.pub.Publish(realtime.CommandTopic(stateTopic), payload); err != nil {
		return err
	}
	c.mu.Lock()
	c.ledOut[stateTopic] = payload
	c.mu.Unlock()
	return nil
}

func (c *Controller) lookupAnchor(co2Topic string) (string, bool) {
	c.mu.Lock()
	if a, ok := c.byCO2[co2Topic]; ok {
		c.mu.Unlock()
		return a, true
	}
	c.mu.Unlock()

	c.RefreshIndex()

	c.mu.Lock()
	defer c.mu.Unlock()
	if a, ok := c.byCO2[co2Topic]; ok {
		return a, true
	}
	c.ignored[co2Topic] = struct{}{}
	return "", false
}

// RefreshIndex пересобирает карту CO₂→якорь (seed / CRUD devices).
func (c *Controller) RefreshIndex() {
	if c.devices == nil {
		return
	}
	devs, err := c.devices.List(context.Background())
	if err != nil {
		c.log.Warn().Err(err).Msg("air list devices")
		return
	}
	next := map[string]string{}
	keepLevel := map[string]Level{}
	for _, d := range devs {
		if !d.Active || d.DeviceType != "air" {
			continue
		}
		anchor := strings.TrimSpace(d.MqttTopic)
		co2 := CO2Topic(anchor)
		if co2 == "" {
			continue
		}
		next[co2] = anchor
	}
	c.mu.Lock()
	for co2, lvl := range c.level {
		if _, ok := next[co2]; ok {
			keepLevel[co2] = lvl
		}
	}
	c.byCO2 = next
	c.level = keepLevel
	c.ignored = map[string]struct{}{}
	c.mu.Unlock()
}

// SyncFromState читает CO₂ из state cache и выставляет LED.
func (c *Controller) SyncFromState(state StateReader) {
	if state == nil {
		return
	}
	c.RefreshIndex()
	c.mu.Lock()
	topics := make([]string, 0, len(c.byCO2))
	for co2 := range c.byCO2 {
		topics = append(topics, co2)
	}
	c.mu.Unlock()
	for _, co2 := range topics {
		payload := strings.TrimSpace(state.StateGet(co2))
		if payload == "" {
			continue
		}
		c.ApplyPayload(co2, payload)
	}
}
