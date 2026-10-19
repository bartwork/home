package mqttbridge

import (
	"context"
	"fmt"
	"sync"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/rs/zerolog"

	"github.com/bartwork/home/internal/discovery"
	"github.com/bartwork/home/internal/floor"
	"github.com/bartwork/home/internal/realtime"
	"github.com/bartwork/home/internal/repository"
)

const (
	defaultSubscribe = "/devices/#"
	localEchoTTL     = 2 * time.Second
)

type Observer interface {
	Observe(topic, payload string)
}

// Fanout delivers each MQTT message to every observer.
type Fanout []Observer

func (f Fanout) Observe(topic, payload string) {
	for _, o := range f {
		if o != nil {
			o.Observe(topic, payload)
		}
	}
}

type Bridge struct {
	log       zerolog.Logger
	hub       *realtime.Hub
	observer  Observer
	shadow    repository.TopicShadowStore
	client    mqtt.Client
	broker    string
	subscribe string

	echoMu sync.Mutex
	echo   map[string]localEcho // stateTopic → last local apply
}

type localEcho struct {
	payload string
	until   time.Time
}

func New(log zerolog.Logger, hub *realtime.Hub, observer Observer, broker, clientID string) *Bridge {
	b := &Bridge{
		log:       log,
		hub:       hub,
		observer:  observer,
		broker:    broker,
		subscribe: defaultSubscribe,
		echo:      make(map[string]localEcho),
	}

	opts := mqtt.NewClientOptions().
		AddBroker(broker).
		SetClientID(clientID).
		SetCleanSession(true).
		SetAutoReconnect(true).
		SetConnectRetry(true).
		SetConnectRetryInterval(3 * time.Second).
		SetKeepAlive(30 * time.Second).
		SetOrderMatters(false)

	opts.SetOnConnectHandler(func(c mqtt.Client) {
		b.log.Info().Str("broker", broker).Str("client", clientID).Str("sub", b.subscribe).Msg("mqtt connected")
		b.hub.SetMQTT(true)
		token := c.Subscribe(b.subscribe, 0, b.onMessage)
		token.Wait()
		if err := token.Error(); err != nil {
			b.log.Error().Err(err).Msg("mqtt subscribe")
		}
		b.republishShadow(c)
	})
	opts.SetConnectionLostHandler(func(_ mqtt.Client, err error) {
		b.log.Warn().Err(err).Msg("mqtt connection lost")
		b.hub.SetMQTT(false)
	})

	b.client = mqtt.NewClient(opts)
	return b
}

func (b *Bridge) SetObserver(observer Observer) { b.observer = observer }
func (b *Bridge) SetShadow(shadow repository.TopicShadowStore) { b.shadow = shadow }

func (b *Bridge) Start() {
	token := b.client.Connect()
	go func() {
		token.Wait()
		if err := token.Error(); err != nil {
			b.log.Warn().Err(err).Str("broker", b.broker).Msg("mqtt connect pending; retry enabled")
		}
	}()
}

func (b *Bridge) Close() {
	if b.client != nil && b.client.IsConnected() {
		b.client.Disconnect(250)
	}
}

// Publish sends to MQTT. Virtual /devices/home/*/on also applies state once locally
// (hub + shadow + observers) and suppresses the broker loopback.
func (b *Bridge) Publish(topic, payload string) error {
	homeState, isHome := floor.StateFromCommand(topic)
	connected := b.client != nil && b.client.IsConnected()

	if connected {
		token := b.client.Publish(topic, 0, isHome, payload)
		token.Wait()
		if err := token.Error(); err != nil {
			return err
		}
		if isHome {
			st := b.client.Publish(homeState, 0, true, payload)
			st.Wait()
		}
	} else if !isHome {
		return fmt.Errorf("mqtt not connected")
	}

	if isHome {
		b.applyHome(homeState, payload, true)
	}
	return nil
}

// RestoreShadow loads persisted home topics into hub/observers (before MQTT connect).
func (b *Bridge) RestoreShadow(ctx context.Context) error {
	if b.shadow == nil {
		return nil
	}
	rows, err := b.shadow.List(ctx)
	if err != nil {
		return err
	}
	for _, row := range rows {
		if !floor.IsControlTopic(row.Topic) {
			continue
		}
		b.applyHome(row.Topic, row.Payload, true)
	}
	return nil
}

func (b *Bridge) applyHome(stateTopic, payload string, markEcho bool) {
	if markEcho {
		b.markLocal(stateTopic, payload)
	}
	b.hub.BroadcastState(stateTopic, payload)
	if b.shadow != nil {
		if err := b.shadow.Upsert(context.Background(), stateTopic, payload); err != nil {
			b.log.Warn().Err(err).Str("topic", stateTopic).Msg("topic shadow upsert")
		}
	}
	if b.observer != nil {
		b.observer.Observe(stateTopic, payload)
	}
}

func (b *Bridge) markLocal(stateTopic, payload string) {
	b.echoMu.Lock()
	b.echo[stateTopic] = localEcho{payload: payload, until: time.Now().Add(localEchoTTL)}
	b.echoMu.Unlock()
}

func (b *Bridge) consumeLocal(stateTopic, payload string) bool {
	b.echoMu.Lock()
	defer b.echoMu.Unlock()
	e, ok := b.echo[stateTopic]
	if !ok {
		return false
	}
	if time.Now().After(e.until) {
		delete(b.echo, stateTopic)
		return false
	}
	if e.payload != payload {
		return false
	}
	delete(b.echo, stateTopic)
	return true
}

func (b *Bridge) republishShadow(c mqtt.Client) {
	if b.shadow == nil {
		return
	}
	rows, err := b.shadow.List(context.Background())
	if err != nil {
		b.log.Warn().Err(err).Msg("list topic shadow")
		return
	}
	for _, row := range rows {
		if !floor.IsControlTopic(row.Topic) {
			continue
		}
		b.markLocal(row.Topic, row.Payload)
		token := c.Publish(row.Topic, 0, true, row.Payload)
		token.Wait()
	}
}

func (b *Bridge) onMessage(_ mqtt.Client, msg mqtt.Message) {
	topic := msg.Topic()
	payload := string(msg.Payload())
	parsed, ok := discovery.ParseTopic(topic)
	if !ok || parsed.Kind != discovery.KindState {
		if b.observer != nil {
			b.observer.Observe(topic, payload)
		}
		return
	}
	if b.consumeLocal(parsed.StateTopic, payload) {
		return
	}
	b.hub.BroadcastState(parsed.StateTopic, payload)
	if floor.IsControlTopic(parsed.StateTopic) && b.shadow != nil {
		_ = b.shadow.Upsert(context.Background(), parsed.StateTopic, payload)
	}
	if b.observer != nil {
		b.observer.Observe(parsed.StateTopic, payload)
	}
}

func (b *Bridge) Connected() bool {
	return b.client != nil && b.client.IsConnected()
}
