package repository

import (
	"context"
	"strings"

	"github.com/bartwork/home/internal/db"
	apperr "github.com/bartwork/home/pkg/errors"
)

// UserStore — контракт доступа к пользователям.
type UserStore interface {
	List(ctx context.Context) ([]User, error)
	Get(ctx context.Context, id int64) (User, error)
	Create(ctx context.Context, in WriteUser) (User, error)
	Update(ctx context.Context, id int64, in WriteUser) (User, error)
	SetPassword(ctx context.Context, id int64, passwordHash string) error
	Delete(ctx context.Context, id int64) error
	GetForAuth(ctx context.Context, login string) (AuthUser, error)
	TouchLastAuth(ctx context.Context, id int64, at string) error
}

type Users struct {
	q *db.Queries
}

func NewUsers(database db.DBTX) *Users {
	return &Users{q: db.New(database)}
}

var _ UserStore = (*Users)(nil)

type User struct {
	ID         int64
	LastName   string
	FirstName  string
	SecondName string
	Email      string
	Phone      string
	LastAuthAt *string
	Active     bool
}

// WriteUser — поля записи (create/update), без id.
type WriteUser struct {
	LastName     string
	FirstName    string
	SecondName   string
	Email        string
	Phone        string
	PasswordHash string // только для Create
	Active       bool
}

type AuthUser struct {
	User
	PasswordHash string
}

func (r *Users) List(ctx context.Context) ([]User, error) {
	rows, err := r.q.ListUsers(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]User, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapUser(row.ID, row.LastName, row.FirstName, row.SecondName, row.Email, row.Phone, row.LastAuthAt, row.IsActive))
	}
	return out, nil
}

func (r *Users) Get(ctx context.Context, id int64) (User, error) {
	row, err := r.q.GetUser(ctx, id)
	if err != nil {
		return User{}, mapDBErr(err)
	}
	return mapUser(row.ID, row.LastName, row.FirstName, row.SecondName, row.Email, row.Phone, row.LastAuthAt, row.IsActive), nil
}

func (r *Users) Create(ctx context.Context, in WriteUser) (User, error) {
	id, err := r.q.CreateUser(ctx, db.CreateUserParams{
		LastName:     in.LastName,
		FirstName:    in.FirstName,
		SecondName:   in.SecondName,
		Email:        in.Email,
		Phone:        in.Phone,
		PasswordHash: in.PasswordHash,
		IsActive:     in.Active,
	})
	if err != nil {
		return User{}, mapDBErr(err)
	}
	return r.Get(ctx, id)
}

func (r *Users) Update(ctx context.Context, id int64, in WriteUser) (User, error) {
	n, err := r.q.UpdateUser(ctx, db.UpdateUserParams{
		LastName:   in.LastName,
		FirstName:  in.FirstName,
		SecondName: in.SecondName,
		Email:      in.Email,
		Phone:      in.Phone,
		IsActive:   in.Active,
		ID:         id,
	})
	if err != nil {
		return User{}, mapDBErr(err)
	}
	if n == 0 {
		return User{}, apperr.ErrNotFound
	}
	return r.Get(ctx, id)
}

func (r *Users) SetPassword(ctx context.Context, id int64, passwordHash string) error {
	n, err := r.q.SetUserPassword(ctx, db.SetUserPasswordParams{
		PasswordHash: passwordHash,
		ID:           id,
	})
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return apperr.ErrNotFound
	}
	return nil
}

func (r *Users) Delete(ctx context.Context, id int64) error {
	n, err := r.q.DeleteUser(ctx, id)
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return apperr.ErrNotFound
	}
	return nil
}

func (r *Users) GetForAuth(ctx context.Context, login string) (AuthUser, error) {
	login = strings.TrimSpace(login)
	row, err := r.q.GetUserForAuth(ctx, db.GetUserForAuthParams{
		Email: login,
		Phone: login,
	})
	if err != nil {
		return AuthUser{}, mapDBErr(err)
	}
	return AuthUser{
		User:         mapUser(row.ID, row.LastName, row.FirstName, row.SecondName, row.Email, row.Phone, row.LastAuthAt, row.IsActive),
		PasswordHash: row.PasswordHash,
	}, nil
}

func (r *Users) TouchLastAuth(ctx context.Context, id int64, at string) error {
	_, err := r.q.TouchLastAuth(ctx, db.TouchLastAuthParams{
		LastAuthAt: &at,
		ID:         id,
	})
	return mapDBErr(err)
}

func mapUser(id int64, lastName, firstName, secondName, email, phone string, lastAuthAt *string, active bool) User {
	return User{
		ID:         id,
		LastName:   lastName,
		FirstName:  firstName,
		SecondName: secondName,
		Email:      email,
		Phone:      phone,
		LastAuthAt: lastAuthAt,
		Active:     active,
	}
}
