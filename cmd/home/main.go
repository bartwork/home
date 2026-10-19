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
	"github.com/bartwork/home/internal/colors"
	"github.com/bartwork/home/internal/config"
	"github.com/bartwork/home/internal/devices"
	"github.com/bartwork/home/internal/discovery"
	"github.com/bartwork/home/internal/floor"
	"github.com/bartwork/home/internal/mqttbridge"
	"github.com/bartwork/home/internal/realtime"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/rooms"
	"github.com/bartwork/home/internal/rules"
	"github.com/bartwork/home/internal/seed"
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

	database, err := store.Open(cfg.DBPath, cfg.SQLCipherKey)
	if err != nil {
		log.Fatal().Err(err).Str("path", cfg.DBPath).Msg("open db")
	}
	defer database.Close()

	usersRepo := repository.NewUsers(database)
	devicesRepo := repository.NewDevices(database)
	discoveredRepo := repository.NewDiscovered(database)
	roomsRepo := repository.NewRooms(database)
	colorsRepo := repository.NewColorFavorites(database)
	rulesRepo := repository.NewRules(database)
	shadowRepo := repository.NewTopicShadows(database)
	roomsSvc := rooms.New(roomsRepo)
	colorsSvc := colors.New(colorsRepo)

	if err := auth.SeedAdmin(ctx, usersRepo, cfg.AdminPassword, cfg.IsProd()); err != nil {
		log.Fatal().Err(err).Msg("seed admin")
	}
	if err := seed.Run(ctx, log.Logger, roomsRepo, devicesRepo); err != nil {
		log.Fatal().Err(err).Msg("seed catalog")
	}

	tokens := auth.NewTokens(cfg.JWTSecret)
	hub := realtime.NewHub(log.Logger)
	discoEngine := discovery.NewEngine(log.Logger, discoveredRepo, devicesRepo)
	discoSvc := discovery.NewService(discoveredRepo, devicesRepo, roomsRepo)

	mqttID := cfg.MQTTClientIDUnique()
	bridge := mqttbridge.New(log.Logger, hub, nil, cfg.MQTTBroker, mqttID)
	bridge.SetShadow(shadowRepo)
	rulesEngine := rules.NewEngine(log.Logger, rulesRepo, bridge)
	rulesSvc := rules.NewService(rulesRepo, rulesEngine)
	bridge.SetObserver(mqttbridge.Fanout{discoEngine, rulesEngine})
	if err := bridge.RestoreShadow(ctx); err != nil {
		log.Warn().Err(err).Msg("restore topic shadow")
	}
	bridge.Start()
	defer bridge.Close()
	if err := rulesEngine.Start(ctx); err != nil {
		log.Fatal().Err(err).Msg("start rules")
	}
	defer rulesEngine.Stop()
	// Publish/Wait на MQTT — в фоне, не блокируем HTTP-listen.
	go floor.Bootstrap(ctx, log.Logger, devicesRepo, shadowRepo, bridge, rulesEngine, func(c context.Context) (int, error) {
		return floor.EnsureRules(c, devicesRepo, rulesRepo, rulesEngine)
	})

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
	discovery.Mount(app, discoSvc)
	rooms.Mount(app, roomsSvc)
	colors.Mount(app, colorsSvc)
	rules.Mount(app, rulesSvc, devicesRepo)
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
			Str("mqttClient", mqttID).
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
