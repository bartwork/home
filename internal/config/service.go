package config

import (
	"fmt"
	"io"
	"os"
	"strings"
	"time"

	"github.com/kelseyhightower/envconfig"
	"github.com/rs/zerolog"
)

const (
	FormatJSON = "json"

	EnvDev  = "dev"
	EnvProd = "prod"

	DevJWTSecret    = "home-dev-jwt-secret"
	DevSQLCipherKey = "home-dev-sqlcipher-key"
)

type CfgService struct {
	LogLevel     string `envconfig:"LOG_LEVEL" default:"info"`
	LogFormat    string `envconfig:"LOG_FORMAT" default:"console"`
	ReportCaller bool   `envconfig:"LOG_REPORT_CALLER" default:"false"`

	// Env: dev (дефолты секретов) или prod (обязательные JWT_SECRET / SQLCIPHER_KEY / ADMIN_PASSWORD при пустой БД).
	Env string `envconfig:"APP_ENV" default:"dev"`

	Bind          string `envconfig:"BIND_SERVICE" default:":9000"`
	MetricsBind   string `envconfig:"BIND_METRICS" default:":9090"`
	LivenessBind  string `envconfig:"BIND_LIVENESS" default:":9091"`
	ReadinessBind string `envconfig:"BIND_READINESS" default:":9092"`

	DBPath string `envconfig:"DB_PATH" default:"data/home.db"`

	JWTSecret     string `envconfig:"JWT_SECRET"`
	SQLCipherKey  string `envconfig:"SQLCIPHER_KEY"`
	AdminPassword string `envconfig:"ADMIN_PASSWORD"`

	MQTTBroker   string `envconfig:"MQTT_BROKER" default:"tcp://127.0.0.1:1883"`
	MQTTClientID string `envconfig:"MQTT_CLIENT_ID" default:"home-controller"`
}

func (cfg CfgService) IsProd() bool {
	e := strings.ToLower(strings.TrimSpace(cfg.Env))
	return e == EnvProd || e == "production"
}

// MQTTClientIDUnique — уникальный id, чтобы два процесса не выбивали друг друга с брокера.
func (cfg CfgService) MQTTClientIDUnique() string {
	id := cfg.MQTTClientID
	if id == "" {
		id = "home-controller"
	}
	return fmt.Sprintf("%s-%d", id, os.Getpid())
}

var service *CfgService

func Service() CfgService {
	if service != nil {
		return *service
	}
	service = &CfgService{}
	if err := envconfig.Process("", service); err != nil {
		panic(err)
	}
	if err := service.applySecrets(); err != nil {
		panic(err)
	}
	return *service
}

func (cfg *CfgService) applySecrets() error {
	if cfg.IsProd() {
		if strings.TrimSpace(cfg.JWTSecret) == "" {
			return fmt.Errorf("JWT_SECRET is required when APP_ENV=prod")
		}
		if cfg.JWTSecret == DevJWTSecret {
			return fmt.Errorf("JWT_SECRET must not use the built-in dev default when APP_ENV=prod")
		}
		if strings.TrimSpace(cfg.SQLCipherKey) == "" {
			return fmt.Errorf("SQLCIPHER_KEY is required when APP_ENV=prod")
		}
		if cfg.SQLCipherKey == DevSQLCipherKey {
			return fmt.Errorf("SQLCIPHER_KEY must not use the built-in dev default when APP_ENV=prod")
		}
		return nil
	}

	if strings.TrimSpace(cfg.JWTSecret) == "" {
		cfg.JWTSecret = DevJWTSecret
	}
	if strings.TrimSpace(cfg.SQLCipherKey) == "" {
		cfg.SQLCipherKey = DevSQLCipherKey
	}
	return nil
}

func (cfg CfgService) Logger() zerolog.Logger {
	level := zerolog.InfoLevel
	if l, err := zerolog.ParseLevel(cfg.LogLevel); err == nil {
		level = l
	}
	var out io.Writer = os.Stdout
	if cfg.LogFormat != FormatJSON {
		out = zerolog.ConsoleWriter{Out: os.Stdout, TimeFormat: time.StampMicro}
	}
	c := zerolog.New(out).Level(level).With().Timestamp()
	if cfg.ReportCaller {
		c = c.Caller()
	}
	return c.Logger()
}
