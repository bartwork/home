package users_test

import (
	"context"
	"path/filepath"
	"testing"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/store"
	"github.com/bartwork/home/internal/users"
	"github.com/bartwork/home/pkg/errors"
)

func TestUsersCRUD(t *testing.T) {
	db, err := store.Open(filepath.Join(t.TempDir(), "home.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = db.Close() })

	svc := users.New(repository.NewUsers(db))
	ctx := context.Background()

	created, err := svc.CreateUser(ctx, dto.CreateUser{
		LastName:   "Иванов",
		FirstName:  "Иван",
		SecondName: "Иванович",
		Email:      "ivan@example.com",
		Phone:      "+79990001122",
		Password:   "secret123",
	})
	if err != nil {
		t.Fatal(err)
	}
	if created.ID == 0 || !created.Active {
		t.Fatalf("create: %+v", created)
	}

	got, err := svc.GetUser(ctx, created.ID)
	if err != nil {
		t.Fatal(err)
	}
	if got.Email != created.Email {
		t.Fatalf("get: %+v", got)
	}

	pass := "new-secret"
	updated, err := svc.UpdateUser(ctx, created.ID, dto.UpdateUser{
		LastName:   "Петров",
		FirstName:  "Пётр",
		SecondName: "Петрович",
		Email:      "petr@example.com",
		Phone:      "+79990001122",
		Password:   &pass,
		Active:     false,
	})
	if err != nil {
		t.Fatal(err)
	}
	if updated.LastName != "Петров" || updated.Active || updated.Email != "petr@example.com" {
		t.Fatalf("update: %+v", updated)
	}

	list, err := svc.ListUsers(ctx)
	if err != nil || len(list) != 1 {
		t.Fatalf("list: err=%v len=%d", err, len(list))
	}

	_, err = svc.CreateUser(ctx, dto.CreateUser{
		Email:    "petr@example.com",
		Phone:    "+70001112233",
		Password: "x",
	})
	if err != errors.ErrConflict {
		t.Fatalf("want email conflict, got %v", err)
	}

	_, err = svc.GetUser(ctx, 0)
	if err != errors.ErrBadRequest {
		t.Fatalf("want bad request, got %v", err)
	}

	if err := svc.DeleteUser(ctx, created.ID); err != nil {
		t.Fatal(err)
	}
	_, err = svc.GetUser(ctx, created.ID)
	if err != errors.ErrNotFound {
		t.Fatalf("want not found, got %v", err)
	}
}
