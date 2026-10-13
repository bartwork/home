package discovery

import (
	"strconv"

	"github.com/bartwork/home/pkg/errors"
	"github.com/gofiber/fiber/v2"
)

func Mount(app *fiber.App, svc *Service) {
	app.Get("/api/v1/discovered", func(c *fiber.Ctx) error {
		items, err := svc.ListNew(c.UserContext())
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"discovered": items})
	})

	app.Get("/api/v1/discovered/count", func(c *fiber.Ctx) error {
		n, err := svc.CountNew(c.UserContext())
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"count": n})
	})

	app.Post("/api/v1/discovered/accept-all", func(c *fiber.Ctx) error {
		typ := c.Query("type")
		n, err := svc.AcceptAll(c.UserContext(), typ)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"accepted": n})
	})

	app.Post("/api/v1/discovered/:id/accept", func(c *fiber.Ctx) error {
		id, err := strconv.ParseInt(c.Params("id"), 10, 64)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		dev, err := svc.Accept(c.UserContext(), id)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"device": dev})
	})

	app.Post("/api/v1/discovered/:id/ignore", func(c *fiber.Ctx) error {
		id, err := strconv.ParseInt(c.Params("id"), 10, 64)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		if err := svc.Ignore(c.UserContext(), id); err != nil {
			return writeErr(c, err)
		}
		return c.SendStatus(fiber.StatusNoContent)
	})
}

func writeErr(c *fiber.Ctx, err error) error {
	if e, ok := err.(errors.Error); ok {
		c.Status(e.Code())
		return c.JSON(e)
	}
	c.Status(fiber.StatusInternalServerError)
	return c.JSON(fiber.Map{"msg": err.Error(), "code": 500})
}
