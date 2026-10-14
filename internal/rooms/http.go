package rooms

import (
	"strconv"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/pkg/errors"
	"github.com/gofiber/fiber/v2"
)

func Mount(app *fiber.App, svc *Service) {
	app.Get("/api/v1/rooms", func(c *fiber.Ctx) error {
		items, err := svc.List(c.UserContext())
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"rooms": items})
	})

	app.Post("/api/v1/rooms", func(c *fiber.Ctx) error {
		var in dto.Room
		if err := c.BodyParser(&in); err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		room, err := svc.Create(c.UserContext(), in)
		if err != nil {
			return writeErr(c, err)
		}
		return c.Status(fiber.StatusCreated).JSON(fiber.Map{"room": room})
	})

	app.Put("/api/v1/rooms/:id", func(c *fiber.Ctx) error {
		id, err := strconv.ParseInt(c.Params("id"), 10, 64)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		var in dto.Room
		if err := c.BodyParser(&in); err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		room, err := svc.Update(c.UserContext(), id, in)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"room": room})
	})

	app.Delete("/api/v1/rooms/:id", func(c *fiber.Ctx) error {
		id, err := strconv.ParseInt(c.Params("id"), 10, 64)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		if err := svc.Delete(c.UserContext(), id); err != nil {
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
