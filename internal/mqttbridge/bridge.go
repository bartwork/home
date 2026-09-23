package mqttbridge

import (
	"fmt"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/rs/zerolog"

	"github.com/bartwork/home/internal/realtime"
)

// Топики Wirenboard живут под /devices/#.
const defaultSubscribe = "/devices/#"

type Bridge struct {
	log       zerolog.Logger
	hub       *realtime.Hub
	client    mqtt.Client
	broker    string
	subscribe string
}

func New(log zerolog.Logger, hub *realtime.Hub, broker, clientID string) *Bridge {
	b := &Bridge{
		log:       log,
		hub:       hub,
		broker:    broker,
		subscribe: defaultSubscribe,
	}

	opts := mqtt.NewClientOptions().
		AddBroker(broker).
		SetClientID(clientID).
		SetAutoReconnect(true).
		SetConnectRetry(true).
		SetConnectRetryInterval(3 * time.Second).
		SetKeepAlive(30 * time.Second).
		SetOrderMatters(false)

	opts.SetOnConnectHandler(func(c mqtt.Client) {
		b.log.Info().Str("broker", broker).Str("sub", b.subscribe).Msg("mqtt connected")
		token := c.Subscribe(b.subscribe, 0, b.onMessage)
		token.Wait()
		if err := token.Error(); err != nil {
			b.log.Error().Err(err).Msg("mqtt subscribe")
		}
	})
	opts.SetConnectionLostHandler(func(_ mqtt.Client, err error) {
		b.log.Warn().Err(err).Msg("mqtt connection lost")
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
	b.hub.BroadcastState(msg.Topic(), string(msg.Payload()))
}
