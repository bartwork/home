package discovery

import (
	"context"

	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
	"github.com/rs/zerolog"
)

// Observer принимает MQTT-сообщения и пишет staging.
type Observer interface {
	Observe(topic, payload string)
}

type Engine struct {
	log   zerolog.Logger
	disco repository.DiscoveredStore
	devs  repository.DeviceStore
}

func NewEngine(log zerolog.Logger, disco repository.DiscoveredStore, devs repository.DeviceStore) *Engine {
	return &Engine{log: log, disco: disco, devs: devs}
}

func (e *Engine) Observe(topic, payload string) {
	parsed, ok := ParseTopic(topic)
	if !ok {
		return
	}
	// Виртуальные уставки/режимы UI (devices/home/…) — не в discovered.
	if parsed.DeviceID == "home" {
		return
	}

	ctx := context.Background()

	// Уже в devices — не трогаем staging (пока устройство не удалят).
	if _, err := e.devs.GetByTopic(ctx, parsed.StateTopic); err == nil {
		return
	} else if err != errors.ErrNotFound {
		e.log.Debug().Err(err).Str("topic", parsed.StateTopic).Msg("discovery get device")
		return
	}

	switch parsed.Kind {
	case KindState:
		// ONOKOM / WB-LED: тип известен по control даже без meta.
		suggested := SuggestDeviceType(parsed.DeviceID, parsed.Control, "")
		if err := e.disco.UpsertState(ctx, repository.WriteDiscovered{
			MqttTopic:     parsed.StateTopic,
			WbDevice:      parsed.DeviceID,
			WbControl:     parsed.Control,
			Title:         DefaultTitle(parsed.DeviceID, parsed.Control),
			SuggestedType: suggested,
			LastPayload:   payload,
		}); err != nil {
			e.log.Debug().Err(err).Str("topic", parsed.StateTopic).Msg("discovery upsert state")
		}
	case KindControlMetaJSON, KindControlMetaField:
		patch := ParseMetaPayload(parsed.Kind, parsed.MetaField, payload)
		title := patch.Title
		if title == "" {
			title = DefaultTitle(parsed.DeviceID, parsed.Control)
		}
		if err := e.disco.UpsertMeta(ctx, repository.WriteDiscovered{
			MqttTopic:     parsed.StateTopic,
			WbDevice:      parsed.DeviceID,
			WbControl:     parsed.Control,
			Title:         title,
			SuggestedType: SuggestDeviceType(parsed.DeviceID, parsed.Control, patch.Type),
			Unit:          patch.Units,
		}); err != nil {
			e.log.Debug().Err(err).Str("topic", parsed.StateTopic).Msg("discovery upsert meta")
		}
	}
}
