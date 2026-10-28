package air

import (
	"context"

	"github.com/rs/zerolog"
)

// Bootstrap: индекс air-устройств → LED по текущему CO₂ из state cache.
func Bootstrap(ctx context.Context, log zerolog.Logger, ctrl *Controller, state StateReader) {
	_ = ctx
	if ctrl == nil {
		return
	}
	ctrl.RefreshIndex()
	ctrl.SyncFromState(state)
	log.Info().Msg("air led bootstrap")
}
