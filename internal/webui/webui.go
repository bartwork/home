package webui

import (
	"embed"
	"io/fs"
	"net/http"
	"strings"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/filesystem"
)

//go:embed all:dist
var dist embed.FS

func Mount(app *fiber.App) error {
	sub, err := fs.Sub(dist, "dist")
	if err != nil {
		return err
	}

	app.Use(filesystem.New(filesystem.Config{
		Root:   http.FS(sub),
		Browse: false,
		MaxAge: 86400,
		Next: func(c *fiber.Ctx) bool {
			path := c.Path()
			return strings.HasPrefix(path, "/api") || !hasFile(sub, path)
		},
	}))

	app.Get("/*", func(c *fiber.Ctx) error {
		if strings.HasPrefix(c.Path(), "/api") {
			return fiber.ErrNotFound
		}
		data, err := fs.ReadFile(sub, "index.html")
		if err != nil {
			return err
		}
		c.Type("html")
		c.Set("Cache-Control", "no-cache")
		return c.Send(data)
	})
	return nil
}

func hasFile(root fs.FS, path string) bool {
	name := strings.TrimPrefix(path, "/")
	if name == "" {
		return false
	}
	f, err := root.Open(name)
	if err != nil {
		return false
	}
	_ = f.Close()
	return true
}
