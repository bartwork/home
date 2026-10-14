package mqttbridge

import (
	"fmt"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/rs/zerolog"

	"github.com/bartwork/home/internal/discovery"
	"github.com/bartwork/home/internal/realtime"
)

const defaultSubscribe = "/devices/#"

type Observer interface {
	Observe(topic, payload string)
}

type Bridge struct {
	log       zerolog.Logger
	hub       *realtime.Hub
	observer  Observer
	client    mqtt.Client
	broker    string
	subscribe string
}

func New(log zerolog.Logger, hub *realtime.Hub, observer Observer, broker, clientID string) *Bridge {
	b := &Bridge{
		log:       log,
		hub:       hub,
		observer:  observer,
		broker:    broker,
		subscribe: defaultSubscribe,
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
	})
	opts.SetConnectionLostHandler(func(_ mqtt.Client, err error) {
		b.log.Warn().Err(err).Msg("mqtt connection lost")
		b.hub.SetMQTT(false)
	})

	b.client = mqtt.NewClient(opts)
	return b
}

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

func (b *Bridge) Publish(topic, payload string) error {
	if b.client == nil || !b.client.IsConnected() {
		return fmt.Errorf("mqtt not connected")
	}
	token := b.client.Publish(topic, 0, false, payload)
	token.Wait()
	return token.Error()
}

func (b *Bridge) onMessage(_ mqtt.Client, msg mqtt.Message) {
	topic := msg.Topic()
	payload := string(msg.Payload())
	if parsed, ok := discovery.ParseTopic(topic); ok && parsed.Kind == discovery.KindState {
		b.hub.BroadcastState(parsed.StateTopic, payload)
	}
	if b.observer != nil {
		b.observer.Observe(topic, payload)
	}
}
