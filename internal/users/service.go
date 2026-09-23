package users

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/usermap"
	"github.com/bartwork/home/pkg/errors"
	"golang.org/x/crypto/bcrypt"
)

type Service struct {
	repo repository.UserStore
}

func New(repo repository.UserStore) *Service {
	return &Service{repo: repo}
}

func (s *Service) ListUsers(ctx context.Context) ([]dto.User, error) {
	rows, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]dto.User, 0, len(rows))
	for _, row := range rows {
		out = append(out, usermap.DTO(row))
	}
	return out, nil
}

func (s *Service) GetUser(ctx context.Context, id int64) (dto.User, error) {
	if id <= 0 {
		return dto.User{}, errors.ErrBadRequest
	}
	row, err := s.repo.Get(ctx, id)
	if err != nil {
		return dto.User{}, err
	}
	return usermap.DTO(row), nil
}

func (s *Service) CreateUser(ctx context.Context, in dto.CreateUser) (dto.User, error) {
	login, email, phone, password, err := normalize(in.Login, in.Email, in.Phone, in.Password, true)
	if err != nil {
		return dto.User{}, err
	}

	hash, err := hashPassword(password)
	if err != nil {
		return dto.User{}, err
	}

	active := true
	if in.Active != nil {
		active = *in.Active
	}

	row, err := s.repo.Create(ctx, repository.WriteUser{
		Login:        login,
		LastName:     strings.TrimSpace(in.LastName),
		FirstName:    strings.TrimSpace(in.FirstName),
		SecondName:   strings.TrimSpace(in.SecondName),
		Email:        email,
		Phone:        phone,
		PasswordHash: hash,
		Active:       active,
	})
	if err != nil {
		return dto.User{}, err
	}
	return usermap.DTO(row), nil
}

func (s *Service) UpdateUser(ctx context.Context, id int64, in dto.UpdateUser) (dto.User, error) {
	if id <= 0 {
		return dto.User{}, errors.ErrBadRequest
	}

	login, email, phone, _, err := normalize(in.Login, in.Email, in.Phone, "", false)
	if err != nil {
		return dto.User{}, err
	}

	row, err := s.repo.Update(ctx, id, repository.WriteUser{
		Login:      login,
		LastName:   strings.TrimSpace(in.LastName),
		FirstName:  strings.TrimSpace(in.FirstName),
		SecondName: strings.TrimSpace(in.SecondName),
		Email:      email,
		Phone:      phone,
		Active:     in.Active,
	})
	if err != nil {
		return dto.User{}, err
	}

	if in.Password != nil {
		password := strings.TrimSpace(*in.Password)
		if password == "" {
			return dto.User{}, errors.ErrBadRequest
		}
		hash, err := hashPassword(password)
		if err != nil {
			return dto.User{}, err
		}
		if err := s.repo.SetPassword(ctx, id, hash); err != nil {
			return dto.User{}, err
		}
	}

	return usermap.DTO(row), nil
}

func (s *Service) DeleteUser(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	return s.repo.Delete(ctx, id)
}

func normalize(login, email, phone, password string, requirePassword bool) (string, string, string, string, error) {
	login = strings.TrimSpace(login)
	email = strings.TrimSpace(email)
	phone = strings.TrimSpace(phone)
	password = strings.TrimSpace(password)
	if login == "" || email == "" || phone == "" {
		return "", "", "", "", errors.ErrBadRequest
	}
	if requirePassword && password == "" {
		return "", "", "", "", errors.ErrBadRequest
	}
	return login, email, phone, password, nil
}

func hashPassword(password string) (string, error) {
	b, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return "", err
	}
	return string(b), nil
}
