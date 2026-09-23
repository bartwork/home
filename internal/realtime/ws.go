package realtime

import (
	"encoding/json"
	"sync"

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
		hub.Register(client)
		defer hub.Unregister(client)

		if b, err := json.Marshal(Envelope{Type: "hello"}); err == nil {
			_ = client.Send(b)
		}

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
			if err := pub.Publish(env.Topic, env.Payload); err != nil {
				log.Debug().Err(err).Str("topic", env.Topic).Msg("mqtt publish from ws")
				if b, err := json.Marshal(Envelope{Type: "error", Topic: env.Topic, Payload: err.Error()}); err == nil {
					_ = client.Send(b)
				}
			}
		}
	}))
}
