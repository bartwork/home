package rules

import (
	"strconv"

	"github.com/bartwork/home/internal/floor"
	"github.com/bartwork/home/pkg/errors"
	"github.com/gofiber/fiber/v2"
)

func Mount(app *fiber.App, svc *Service, devices floor.DeviceLister) {
	app.Get("/api/v1/rules", func(c *fiber.Ctx) error {
		items, err := svc.List(c.UserContext())
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"rules": items})
	})

	app.Post("/api/v1/rules/ensure-floors", func(c *fiber.Ctx) error {
		if devices == nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		n, err := floor.EnsureRules(c.UserContext(), devices, svc.repo, svc.engine)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"created": n})
	})

	app.Post("/api/v1/rules", func(c *fiber.Ctx) error {
		var in WriteIn
		if err := c.BodyParser(&in); err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		item, err := svc.Create(c.UserContext(), in)
		if err != nil {
			return writeErr(c, err)
		}
		return c.Status(fiber.StatusCreated).JSON(fiber.Map{"rule": item})
	})

	app.Get("/api/v1/rules/:id", func(c *fiber.Ctx) error {
		id, err := parseID(c)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		item, err := svc.Get(c.UserContext(), id)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"rule": item})
	})

	app.Put("/api/v1/rules/:id", func(c *fiber.Ctx) error {
		id, err := parseID(c)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		var in WriteIn
		if err := c.BodyParser(&in); err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		item, err := svc.Update(c.UserContext(), id, in)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"rule": item})
	})

	app.Delete("/api/v1/rules/:id", func(c *fiber.Ctx) error {
		id, err := parseID(c)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		if err := svc.Delete(c.UserContext(), id); err != nil {
			return writeErr(c, err)
		}
		return c.SendStatus(fiber.StatusNoContent)
	})

	app.Post("/api/v1/rules/:id/enable", func(c *fiber.Ctx) error {
		id, err := parseID(c)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		item, err := svc.SetEnabled(c.UserContext(), id, true)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"rule": item})
	})

	app.Post("/api/v1/rules/:id/disable", func(c *fiber.Ctx) error {
		id, err := parseID(c)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		item, err := svc.SetEnabled(c.UserContext(), id, false)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"rule": item})
	})

	app.Post("/api/v1/rules/:id/run", func(c *fiber.Ctx) error {
		id, err := parseID(c)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		if err := svc.Run(c.UserContext(), id); err != nil {
			return writeErr(c, err)
		}
		return c.SendStatus(fiber.StatusAccepted)
	})
}

func parseID(c *fiber.Ctx) (int64, error) {
	return strconv.ParseInt(c.Params("id"), 10, 64)
}

func writeErr(c *fiber.Ctx, err error) error {
	if e, ok := err.(errors.Error); ok {
		c.Status(e.Code())
		return c.JSON(e)
	}
	c.Status(fiber.StatusInternalServerError)
	return c.JSON(fiber.Map{"msg": err.Error(), "code": 500})
}
