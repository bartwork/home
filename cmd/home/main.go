package main

import (
	"context"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"
	"time"

	_ "github.com/KimMachineGun/automemlimit"
	"github.com/rs/zerolog/log"
	_ "go.uber.org/automaxprocs"

	"github.com/bartwork/home/internal/auth"
	"github.com/bartwork/home/internal/config"
	"github.com/bartwork/home/internal/devices"
	"github.com/bartwork/home/internal/mqttbridge"
	"github.com/bartwork/home/internal/realtime"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/store"
	"github.com/bartwork/home/internal/transport"
	"github.com/bartwork/home/internal/users"
	"github.com/bartwork/home/internal/utils"
	"github.com/bartwork/home/internal/utils/header"
	"github.com/bartwork/home/internal/webui"
)

func main() {
	cfg := config.Service()
	log.Logger = cfg.Logger()
	ctx := log.Logger.WithContext(context.Background())

	if err := os.MkdirAll(filepath.Dir(cfg.DBPath), 0o755); err != nil {
		log.Fatal().Err(err).Msg("mkdir db")
	}

	database, err := store.Open(cfg.DBPath)
	if err != nil {
		log.Fatal().Err(err).Str("path", cfg.DBPath).Msg("open db")
	}
	defer database.Close()

	usersRepo := repository.NewUsers(database)
	devicesRepo := repository.NewDevices(database)
	if err := auth.SeedAdmin(ctx, usersRepo); err != nil {
		log.Fatal().Err(err).Msg("seed admin")
	}
	if err := devices.SeedDemo(ctx, devicesRepo); err != nil {
		log.Fatal().Err(err).Msg("seed devices")
	}

	tokens := auth.NewTokens(cfg.JWTSecret)
	hub := realtime.NewHub(log.Logger)
	bridge := mqttbridge.New(log.Logger, hub, cfg.MQTTBroker, cfg.MQTTClientID)
	bridge.Start()
	defer bridge.Close()

	liveness := utils.NewHealth("/liveness", "ok")
	liveness.Start(ctx, cfg.LivenessBind)
	defer liveness.Stop(ctx)

	readiness := utils.NewHealth("/readiness", "ok")
	readiness.Start(ctx, cfg.ReadinessBind)
	defer readiness.Stop(ctx)

	srv := transport.New(log.Logger,
		transport.Use(auth.Middleware(tokens)),
		transport.WithRequestID(header.XRequestID.String()),
		transport.Auth(transport.NewAuth(auth.New(usersRepo, tokens))),
		transport.Users(transport.NewUsers(users.New(usersRepo))),
		transport.Devices(transport.NewDevices(devices.New(devicesRepo))),
	).WithMetrics().WithLog()

	app := srv.Fiber()
	realtime.MountWS(app, hub, bridge, log.Logger)
	if err := webui.Mount(app); err != nil {
		log.Fatal().Err(err).Msg("mount webui")
	}

	srv.ServeMetrics(log.Logger, "/", cfg.MetricsBind)

	go func() {
		log.Info().
			Str("bind", cfg.Bind).
			Str("db", cfg.DBPath).
			Str("mqtt", cfg.MQTTBroker).
			Msg("listen")
		if err := app.Listen(cfg.Bind); err != nil {
			log.Error().Err(err).Msg("listen stopped")
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	log.Info().Msg("shutdown")
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = app.ShutdownWithContext(shutdownCtx)
}
