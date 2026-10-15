package auth_test

import (
	"context"
	"path/filepath"
	"testing"

	"github.com/bartwork/home/internal/auth"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/store"
)

func TestSeedAdminProdRequiresPassword(t *testing.T) {
	db, err := store.OpenDev(filepath.Join(t.TempDir(), "home.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = db.Close() })
	repo := repository.NewUsers(db)
	if err := auth.SeedAdmin(context.Background(), repo, "", true); err == nil {
		t.Fatal("expected ADMIN_PASSWORD required")
	}
	if err := auth.SeedAdmin(context.Background(), repo, auth.DefaultAdminPassword, true); err == nil {
		t.Fatal("expected reject of default admin password in prod")
	}
	if err := auth.SeedAdmin(context.Background(), repo, "s3cret-prod", true); err != nil {
		t.Fatal(err)
	}
}
