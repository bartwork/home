package discovery

import (
	"strconv"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/pkg/errors"
	"github.com/gofiber/fiber/v2"
)

func Mount(app *fiber.App, svc *Service) {
	app.Get("/api/v1/discovered", func(c *fiber.Ctx) error {
		status := strings.ToLower(strings.TrimSpace(c.Query("status")))
		var (
			items []dto.Discovered
			err   error
		)
		switch status {
		case "", "new":
			items, err = svc.ListNew(c.UserContext())
		case "ignored":
			items, err = svc.ListIgnored(c.UserContext())
		default:
			return writeErr(c, errors.ErrBadRequest)
		}
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
		ignoreRaw := c.Query("ignoreRaw") != "0"
		accepted, ignored, failed, err := svc.AcceptAll(c.UserContext(), typ, ignoreRaw)
		if err != nil {
			return writeErr(c, err)
		}
		return c.JSON(fiber.Map{"accepted": accepted, "ignored": ignored, "failed": failed})
	})

	app.Post("/api/v1/discovered/:id/accept", func(c *fiber.Ctx) error {
		id, err := strconv.ParseInt(c.Params("id"), 10, 64)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		var body struct {
			Name   string `json:"name"`
			RoomID int64  `json:"roomId"`
			Icon   string `json:"icon"`
		}
		_ = c.BodyParser(&body)
		dev, err := svc.Accept(c.UserContext(), id, AcceptOpts{
			Name: body.Name, RoomID: body.RoomID, Icon: body.Icon,
		})
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

	app.Post("/api/v1/discovered/:id/restore", func(c *fiber.Ctx) error {
		id, err := strconv.ParseInt(c.Params("id"), 10, 64)
		if err != nil {
			return writeErr(c, errors.ErrBadRequest)
		}
		if err := svc.Restore(c.UserContext(), id); err != nil {
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
