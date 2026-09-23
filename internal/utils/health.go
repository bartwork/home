package utils

import (
	"context"

	"github.com/gofiber/fiber/v2"
	"github.com/rs/zerolog/log"
)

type Health struct {
	srv *fiber.App
}

func NewHealth(path string, response any) Health {
	h := Health{srv: fiber.New(fiber.Config{DisableStartupMessage: true})}
	h.srv.Get(path, func(c *fiber.Ctx) error {
		return c.JSON(response)
	})
	return h
}

func (h Health) Start(ctx context.Context, address string) {
	go func() {
		if err := h.srv.Listen(address); err != nil {
			log.Ctx(ctx).Panic().Err(err).Str("addr", address).Msg("health listen")
		}
	}()
}

func (h Health) Stop(ctx context.Context) {
	if h.srv == nil {
		return
	}
	if err := h.srv.Shutdown(); err != nil {
		log.Ctx(ctx).Error().Err(err).Msg("health shutdown")
	}
}
