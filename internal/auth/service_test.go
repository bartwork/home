package auth_test

import (
	"context"
	"path/filepath"
	"testing"

	"github.com/bartwork/home/internal/auth"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/store"
	"github.com/bartwork/home/pkg/errors"
)

func TestLoginSeedAdmin(t *testing.T) {
	db, err := store.Open(filepath.Join(t.TempDir(), "home.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = db.Close() })

	repo := repository.NewUsers(db)
	ctx := context.Background()
	if err := auth.SeedAdmin(ctx, repo); err != nil {
		t.Fatal(err)
	}

	svc := auth.New(repo, auth.NewTokens("test-secret"))
	token, user, err := svc.Login(ctx, auth.DefaultAdminEmail, auth.DefaultAdminPassword)
	if err != nil {
		t.Fatal(err)
	}
	if token == "" || user.Email != auth.DefaultAdminEmail {
		t.Fatalf("unexpected: token=%q user=%+v", token, user)
	}

	_, _, err = svc.Login(ctx, auth.DefaultAdminEmail, "wrong")
	if err != errors.ErrUnauthorized {
		t.Fatalf("want unauthorized, got %v", err)
	}
}
