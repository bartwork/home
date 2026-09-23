package auth

import (
	"context"
	"strings"
	"time"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/usermap"
	"github.com/bartwork/home/pkg/errors"
	"golang.org/x/crypto/bcrypt"
)

type Service struct {
	repo   repository.UserStore
	tokens *Tokens
}

func New(repo repository.UserStore, tokens *Tokens) *Service {
	return &Service{repo: repo, tokens: tokens}
}

func (s *Service) Login(ctx context.Context, login, password string) (token string, user dto.User, err error) {
	login = strings.TrimSpace(login)
	if login == "" || password == "" {
		return "", dto.User{}, errors.ErrBadRequest
	}

	row, err := s.repo.GetForAuth(ctx, login)
	if err != nil {
		if err == errors.ErrNotFound {
			return "", dto.User{}, errors.ErrUnauthorized
		}
		return "", dto.User{}, err
	}
	if !row.Active {
		return "", dto.User{}, errors.ErrForbidden
	}
	if err := bcrypt.CompareHashAndPassword([]byte(row.PasswordHash), []byte(password)); err != nil {
		return "", dto.User{}, errors.ErrUnauthorized
	}

	now := time.Now().UTC().Format(time.RFC3339)
	_ = s.repo.TouchLastAuth(ctx, row.ID, now)

	token, err = s.tokens.Issue(row.ID)
	if err != nil {
		return "", dto.User{}, err
	}

	u := usermap.DTO(row.User)
	u.LastAuthAt = now
	return token, u, nil
}
