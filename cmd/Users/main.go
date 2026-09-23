package main

import (
	"context"
	"os"
	"os/signal"
	"syscall"

	_ "github.com/KimMachineGun/automemlimit"
	"github.com/rs/zerolog/log"
	_ "go.uber.org/automaxprocs"

	"github.com/bartwork/home/internal/services/users"
	"github.com/bartwork/home/internal/transport"
	"github.com/bartwork/home/internal/utils"
	"github.com/bartwork/home/internal/utils/header"

	"github.com/bartwork/home/internal/config"
)

const (
	serviceName = "users"
)

func main() {

	log.Logger = config.Service().Logger()
	ctx := log.Logger.WithContext(context.Background())

	shutdown := make(chan os.Signal, 1)
	signal.Notify(shutdown, syscall.SIGINT)

	log.Info().Str("service", serviceName).Msg("start service")
	defer log.Info().Msg("shutdown server")

	liveness := utils.NewHealth("/liveness", "ok")
	liveness.Start(ctx, config.Service().LivenessBind)
	defer liveness.Stop(ctx)

	svcUsers := users.New()

	services := []transport.Option{
		transport.WithRequestID(header.XRequestID.String()),
		transport.Users(transport.NewUsers(svcUsers)),
	}

	srv := transport.New(log.Logger, services...).WithMetrics().WithLog()

	readiness := utils.NewHealth("/readiness", "ok")
	readiness.Start(ctx, config.Service().ReadinessBind)
	defer readiness.Stop(ctx)

	srv.ServeMetrics(log.Logger, "/", config.Service().MetricsBind)

	go func() {
		log.Info().Str("bind", config.Service().Bind).Msg("listen on")
		if err := srv.Fiber().Listen(config.Service().Bind); err != nil {
			log.Panic().Err(err).Stack().Msg("server error")
		}
	}()

	<-shutdown
}
