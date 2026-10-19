package rules

import "sync"

type topicState struct {
	mu   sync.RWMutex
	last map[string]string
}

func newTopicState() *topicState {
	return &topicState{last: make(map[string]string)}
}

func (s *topicState) Set(topic, payload string) (prev string, changed bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	prev, ok := s.last[topic]
	if ok && prev == payload {
		return prev, false
	}
	s.last[topic] = payload
	return prev, true
}

func (s *topicState) Get(topic string) string {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.last[topic]
}

func (s *topicState) Snapshot() map[string]string {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make(map[string]string, len(s.last))
	for k, v := range s.last {
		out[k] = v
	}
	return out
}
