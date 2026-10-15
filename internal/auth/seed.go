package auth

import (
	"context"
	"fmt"
	"strings"

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
// В prod password обязателен (из ADMIN_PASSWORD); в dev пустой password → "admin".
func SeedAdmin(ctx context.Context, repo repository.UserStore, password string, prod bool) error {
	users, err := repo.List(ctx)
	if err != nil {
		return err
	}
	if len(users) > 0 {
		return nil
	}

	password = strings.TrimSpace(password)
	if password == "" {
		if prod {
			return fmt.Errorf("ADMIN_PASSWORD is required when APP_ENV=prod and users table is empty")
		}
		password = DefaultAdminPassword
	}
	if prod && password == DefaultAdminPassword {
		return fmt.Errorf("ADMIN_PASSWORD must not be the built-in dev default when APP_ENV=prod")
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
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
