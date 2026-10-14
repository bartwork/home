package realtime

import (
	"encoding/json"
	"sync"

	"github.com/rs/zerolog"
)

type Envelope struct {
	Type    string `json:"type"`
	Topic   string `json:"topic,omitempty"`
	Payload string `json:"payload,omitempty"`
}

type Client interface {
	Send([]byte) error
}

type Hub struct {
	log     zerolog.Logger
	mu      sync.RWMutex
	clients map[Client]struct{}
	last    map[string]string
	mqttOK  bool
}

func NewHub(log zerolog.Logger) *Hub {
	return &Hub{
		log:     log,
		clients: make(map[Client]struct{}),
		last:    make(map[string]string),
	}
}

func (h *Hub) Register(c Client) {
	h.mu.Lock()
	h.clients[c] = struct{}{}
	mqttOK := h.mqttOK
	states := make([]Envelope, 0, len(h.last))
	for topic, payload := range h.last {
		states = append(states, Envelope{Type: "state", Topic: topic, Payload: payload})
	}
	h.mu.Unlock()

	h.send(c, mqttEnvelope(mqttOK))
	for _, env := range states {
		h.send(c, env)
	}
}

func (h *Hub) Unregister(c Client) {
	h.mu.Lock()
	delete(h.clients, c)
	h.mu.Unlock()
}

func (h *Hub) SetMQTT(ok bool) {
	h.mu.Lock()
	if h.mqttOK == ok {
		h.mu.Unlock()
		return
	}
	h.mqttOK = ok
	clients := h.clientListLocked()
	h.mu.Unlock()

	env := mqttEnvelope(ok)
	for _, c := range clients {
		h.send(c, env)
	}
}

func (h *Hub) MQTTConnected() bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return h.mqttOK
}

func (h *Hub) BroadcastState(topic, payload string) {
	h.mu.Lock()
	h.last[topic] = payload
	clients := h.clientListLocked()
	h.mu.Unlock()

	env := Envelope{Type: "state", Topic: topic, Payload: payload}
	for _, c := range clients {
		h.send(c, env)
	}
}

func (h *Hub) clientListLocked() []Client {
	clients := make([]Client, 0, len(h.clients))
	for c := range h.clients {
		clients = append(clients, c)
	}
	return clients
}

func (h *Hub) send(c Client, env Envelope) {
	b, err := json.Marshal(env)
	if err != nil {
		return
	}
	if err := c.Send(b); err != nil {
		h.log.Debug().Err(err).Str("type", env.Type).Msg("ws send")
	}
}

func mqttEnvelope(ok bool) Envelope {
	payload := "0"
	if ok {
		payload = "1"
	}
	return Envelope{Type: "mqtt", Payload: payload}
}
