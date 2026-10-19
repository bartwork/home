package floor

import (
	"context"
	"strings"

	"github.com/bartwork/home/internal/repository"
	"github.com/rs/zerolog"
)

type ShadowStore interface {
	List(ctx context.Context) ([]repository.TopicShadow, error)
}

type Publisher interface {
	Publish(topic, payload string) error
}

type Engine interface {
	Reload(ctx context.Context) error
	StateGet(topic string) string
	SyncThermostats()
}

// Bootstrap: ensure rules → seed missing setpoint/mode → sync реле по текущему state.
func Bootstrap(
	ctx context.Context,
	log zerolog.Logger,
	devices DeviceLister,
	shadow ShadowStore,
	pub Publisher,
	engine Engine,
	ensure func(context.Context) (int, error),
) {
	n, err := ensure(ctx)
	if err != nil {
		log.Warn().Err(err).Msg("ensure floor rules")
	} else if n > 0 {
		log.Info().Int("created", n).Msg("floor thermostat rules")
	}

	devs, err := devices.List(ctx)
	if err != nil {
		log.Warn().Err(err).Msg("list devices for floor defaults")
		return
	}

	shadowSet := map[string]string{}
	if shadow != nil {
		if rows, err := shadow.List(ctx); err == nil {
			for _, row := range rows {
				shadowSet[row.Topic] = row.Payload
			}
		}
	}

	for _, d := range devs {
		if !d.Active || d.DeviceType != "floor" {
			continue
		}
		seedDefaults(d.ID, shadowSet, engine, pub, log)
	}

	engine.SyncThermostats()
}

func seedDefaults(deviceID int64, shadow map[string]string, engine Engine, pub Publisher, log zerolog.Logger) {
	if pub == nil {
		return
	}
	sp := SetpointTopic(deviceID)
	mode := ModeTopic(deviceID)
	if strings.TrimSpace(shadow[sp]) == "" && strings.TrimSpace(engine.StateGet(sp)) == "" {
		if err := pub.Publish(CommandTopic(sp), DefaultSetpoint); err != nil {
			log.Warn().Err(err).Str("topic", sp).Msg("seed floor setpoint")
		}
	}
	if strings.TrimSpace(shadow[mode]) == "" && strings.TrimSpace(engine.StateGet(mode)) == "" {
		if err := pub.Publish(CommandTopic(mode), DefaultMode); err != nil {
			log.Warn().Err(err).Str("topic", mode).Msg("seed floor mode")
		}
	}
}
