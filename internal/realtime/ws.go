package realtime

import (
	"encoding/json"
	"strings"
	"sync"

	"github.com/bartwork/home/internal/floor"
	"github.com/gofiber/contrib/websocket"
	"github.com/gofiber/fiber/v2"
	"github.com/rs/zerolog"
)

type wsClient struct {
	conn *websocket.Conn
	mu   sync.Mutex
}

func (c *wsClient) Send(b []byte) error {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.conn.WriteMessage(websocket.TextMessage, b)
}

type Publisher interface {
	Publish(topic, payload string) error
}

func MountWS(app *fiber.App, hub *Hub, pub Publisher, log zerolog.Logger) {
	app.Use("/api/v1/ws", func(c *fiber.Ctx) error {
		if websocket.IsWebSocketUpgrade(c) {
			return c.Next()
		}
		return fiber.ErrUpgradeRequired
	})

	app.Get("/api/v1/ws", websocket.New(func(conn *websocket.Conn) {
		client := &wsClient{conn: conn}
		if b, err := json.Marshal(Envelope{Type: "hello"}); err == nil {
			_ = client.Send(b)
		}
		hub.Register(client)
		defer hub.Unregister(client)

		for {
			_, data, err := conn.ReadMessage()
			if err != nil {
				return
			}
			var env Envelope
			if err := json.Unmarshal(data, &env); err != nil {
				continue
			}
			if env.Type != "cmd" || env.Topic == "" {
				continue
			}
			cmdTopic := CommandTopic(env.Topic)
			// Virtual /devices/home/* may work offline (local echo + shadow).
			homeCmd := isHomeCommandTopic(cmdTopic)
			if !hub.MQTTConnected() && !homeCmd {
				_ = client.Send(mustJSON(Envelope{
					Type: "error", Topic: env.Topic, Payload: "mqtt not connected",
				}))
				continue
			}
			if err := pub.Publish(cmdTopic, env.Payload); err != nil {
				log.Warn().Err(err).Str("topic", cmdTopic).Str("payload", env.Payload).Msg("mqtt publish from ws")
				_ = client.Send(mustJSON(Envelope{
					Type: "error", Topic: env.Topic, Payload: err.Error(),
				}))
				continue
			}
			log.Debug().Str("topic", cmdTopic).Str("payload", env.Payload).Msg("mqtt cmd")
		}
	}))
}

func mustJSON(env Envelope) []byte {
	b, err := json.Marshal(env)
	if err != nil {
		return []byte(`{"type":"error","payload":"encode"}`)
	}
	return b
}

func isHomeCommandTopic(cmdTopic string) bool {
	_, ok := floor.StateFromCommand(strings.TrimSpace(cmdTopic))
	return ok
}
