package auth

import (
	"strings"

	"github.com/bartwork/home/pkg/errors"
	"github.com/gofiber/fiber/v2"
)

const LocalsUserID = "userID"

// Middleware защищает /api/* JWT-ом, кроме login.
func Middleware(tokens *Tokens) fiber.Handler {
	return func(c *fiber.Ctx) error {
		path := c.Path()
		if !strings.HasPrefix(path, "/api/") {
			return c.Next()
		}
		if path == "/api/v1/auth/login" {
			return c.Next()
		}

		token := bearerToken(c.Get("Authorization"))
		if token == "" {
			token = c.Query("token")
		}
		if token == "" {
			return writeErr(c, errors.ErrUnauthorized)
		}

		claims, err := tokens.Parse(token)
		if err != nil || claims.UserID <= 0 {
			return writeErr(c, errors.ErrUnauthorized)
		}

		c.Locals(LocalsUserID, claims.UserID)
		return c.Next()
	}
}

func bearerToken(header string) string {
	const prefix = "Bearer "
	if strings.HasPrefix(header, prefix) {
		return strings.TrimSpace(header[len(prefix):])
	}
	return ""
}

func writeErr(c *fiber.Ctx, err errors.Error) error {
	c.Status(err.Code())
	return c.JSON(err)
}

func UserIDFromLocals(c *fiber.Ctx) (int64, bool) {
	v := c.Locals(LocalsUserID)
	id, ok := v.(int64)
	return id, ok && id > 0
}
