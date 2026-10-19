package mqttbridge

import (
	"testing"
	"time"
)

func TestLocalEchoConsumeOnce(t *testing.T) {
	b := &Bridge{echo: make(map[string]localEcho)}
	topic := "/devices/home/controls/floor_1_setpoint"
	b.markLocal(topic, "26")
	if !b.consumeLocal(topic, "26") {
		t.Fatal("expected consume")
	}
	if b.consumeLocal(topic, "26") {
		t.Fatal("second consume should miss")
	}
}

func TestLocalEchoTTL(t *testing.T) {
	b := &Bridge{echo: make(map[string]localEcho)}
	topic := "/devices/home/controls/floor_1_mode"
	b.echoMu.Lock()
	b.echo[topic] = localEcho{payload: "auto", until: time.Now().Add(-time.Second)}
	b.echoMu.Unlock()
	if b.consumeLocal(topic, "auto") {
		t.Fatal("expired echo must not consume")
	}
}

func TestLocalEchoPayloadMismatch(t *testing.T) {
	b := &Bridge{echo: make(map[string]localEcho)}
	topic := "/devices/home/controls/floor_1_setpoint"
	b.markLocal(topic, "26")
	if b.consumeLocal(topic, "27") {
		t.Fatal("mismatch must not consume")
	}
	if !b.consumeLocal(topic, "26") {
		t.Fatal("original still consumable")
	}
}
