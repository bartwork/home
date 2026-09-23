package auth

import (
	"context"

	"github.com/bartwork/home/internal/repository"
	"golang.org/x/crypto/bcrypt"
)

const (
	DefaultAdminLogin    = "admin"
	DefaultAdminEmail    = "admin@home.local"
	DefaultAdminPhone    = "+70000000000"
	DefaultAdminPassword = "admin"
)

// SeedAdmin создаёт первого пользователя, если таблица пуста.
func SeedAdmin(ctx context.Context, repo repository.UserStore) error {
	users, err := repo.List(ctx)
	if err != nil {
		return err
	}
	if len(users) > 0 {
		return nil
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(DefaultAdminPassword), bcrypt.DefaultCost)
	if err != nil {
		return err
	}

	_, err = repo.Create(ctx, repository.WriteUser{
		Login:        DefaultAdminLogin,
		LastName:     "Админ",
		FirstName:    "Системный",
		Email:        DefaultAdminEmail,
		Phone:        DefaultAdminPhone,
		PasswordHash: string(hash),
		Active:       true,
	})
	return err
}
