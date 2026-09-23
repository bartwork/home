package realtime

import (
	"encoding/json"
	"sync"

	"github.com/rs/zerolog"
)

type Envelope struct {
	Type     string `json:"type"`
	Topic    string `json:"topic,omitempty"`
	Payload  string `json:"payload,omitempty"`
	DeviceID int64  `json:"deviceId,omitempty"`
}

type Client interface {
	Send([]byte) error
}

type Hub struct {
	log     zerolog.Logger
	mu      sync.RWMutex
	clients map[Client]struct{}
	last    map[string]string
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
	snapshot := make([]Envelope, 0, len(h.last))
	for topic, payload := range h.last {
		snapshot = append(snapshot, Envelope{Type: "state", Topic: topic, Payload: payload})
	}
	h.mu.Unlock()

	for _, env := range snapshot {
		b, err := json.Marshal(env)
		if err != nil {
			continue
		}
		_ = c.Send(b)
	}
}

func (h *Hub) Unregister(c Client) {
	h.mu.Lock()
	delete(h.clients, c)
	h.mu.Unlock()
}

func (h *Hub) BroadcastState(topic, payload string) {
	h.mu.Lock()
	h.last[topic] = payload
	clients := make([]Client, 0, len(h.clients))
	for c := range h.clients {
		clients = append(clients, c)
	}
	h.mu.Unlock()

	b, err := json.Marshal(Envelope{Type: "state", Topic: topic, Payload: payload})
	if err != nil {
		return
	}
	for _, c := range clients {
		if err := c.Send(b); err != nil {
			h.log.Debug().Err(err).Msg("ws send")
		}
	}
}
