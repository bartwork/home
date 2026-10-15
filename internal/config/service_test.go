package config

import "testing"

func TestApplySecretsDevDefaults(t *testing.T) {
	cfg := CfgService{Env: "dev"}
	if err := cfg.applySecrets(); err != nil {
		t.Fatal(err)
	}
	if cfg.JWTSecret != DevJWTSecret || cfg.SQLCipherKey != DevSQLCipherKey {
		t.Fatalf("dev defaults: %+v", cfg)
	}
}

func TestApplySecretsProdRequires(t *testing.T) {
	cfg := CfgService{Env: "prod"}
	if err := cfg.applySecrets(); err == nil {
		t.Fatal("expected error without secrets")
	}
	cfg = CfgService{Env: "prod", JWTSecret: DevJWTSecret, SQLCipherKey: "ok-key-not-dev"}
	if err := cfg.applySecrets(); err == nil {
		t.Fatal("expected error on dev JWT")
	}
	cfg = CfgService{Env: "prod", JWTSecret: "prod-jwt", SQLCipherKey: "prod-sql-key"}
	if err := cfg.applySecrets(); err != nil {
		t.Fatal(err)
	}
}
