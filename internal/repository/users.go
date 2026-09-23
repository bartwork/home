package repository

import (
	"context"
	"database/sql"
	"errors"
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

func (r *Users) List(ctx context.Context) ([]User, error) {
	rows, err := r.q.ListUsers(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]User, 0, len(rows))
	for _, row := range rows {
		out = append(out, User{
			ID: row.ID, LastName: row.LastName, FirstName: row.FirstName, SecondName: row.SecondName,
			Email: row.Email, Phone: row.Phone, LastAuthAt: row.LastAuthAt, Active: row.IsActive,
		})
	}
	return out, nil
}

func (r *Users) Get(ctx context.Context, id int64) (User, error) {
	row, err := r.q.GetUser(ctx, id)
	if err != nil {
		return User{}, mapDBErr(err)
	}
	return User{
		ID: row.ID, LastName: row.LastName, FirstName: row.FirstName, SecondName: row.SecondName,
		Email: row.Email, Phone: row.Phone, LastAuthAt: row.LastAuthAt, Active: row.IsActive,
	}, nil
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

func mapDBErr(err error) error {
	if errors.Is(err, sql.ErrNoRows) {
		return apperr.ErrNotFound
	}
	if strings.Contains(err.Error(), "UNIQUE constraint failed") {
		return apperr.ErrConflict
	}
	return err
}
