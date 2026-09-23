package main

import (
	"context"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"

	_ "github.com/KimMachineGun/automemlimit"
	"github.com/rs/zerolog/log"
	_ "go.uber.org/automaxprocs"

	"github.com/bartwork/home/internal/config"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/store"
	"github.com/bartwork/home/internal/transport"
	"github.com/bartwork/home/internal/users"
	"github.com/bartwork/home/internal/utils"
	"github.com/bartwork/home/internal/utils/header"
)

func main() {
	cfg := config.Service()
	log.Logger = cfg.Logger()
	ctx := log.Logger.WithContext(context.Background())

	shutdown := make(chan os.Signal, 1)
	signal.Notify(shutdown, syscall.SIGINT, syscall.SIGTERM)

	log.Info().Msg("start")
	defer log.Info().Msg("stop")

	if err := os.MkdirAll(filepath.Dir(cfg.DBPath), 0o755); err != nil {
		log.Fatal().Err(err).Msg("mkdir db")
	}

	database, err := store.Open(cfg.DBPath)
	if err != nil {
		log.Fatal().Err(err).Str("path", cfg.DBPath).Msg("open db")
	}
	defer database.Close()

	liveness := utils.NewHealth("/liveness", "ok")
	liveness.Start(ctx, cfg.LivenessBind)
	defer liveness.Stop(ctx)

	usersSvc := users.New(repository.NewUsers(database))

	srv := transport.New(log.Logger,
		transport.WithRequestID(header.XRequestID.String()),
		transport.Users(transport.NewUsers(usersSvc)),
	).WithMetrics().WithLog()

	readiness := utils.NewHealth("/readiness", "ok")
	readiness.Start(ctx, cfg.ReadinessBind)
	defer readiness.Stop(ctx)

	srv.ServeMetrics(log.Logger, "/", cfg.MetricsBind)

	go func() {
		log.Info().Str("bind", cfg.Bind).Str("db", cfg.DBPath).Msg("listen")
		if err := srv.Fiber().Listen(cfg.Bind); err != nil {
			log.Panic().Err(err).Msg("listen")
		}
	}()

	<-shutdown
}
