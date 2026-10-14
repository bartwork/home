package repository

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/db"
	apperr "github.com/bartwork/home/pkg/errors"
)

type RoomStore interface {
	List(ctx context.Context) ([]Room, error)
	Get(ctx context.Context, id int64) (Room, error)
	Create(ctx context.Context, in WriteRoom) (Room, error)
	Update(ctx context.Context, id int64, in WriteRoom) (Room, error)
	Delete(ctx context.Context, id int64) error
}

type Rooms struct {
	q *db.Queries
}

func NewRooms(database db.DBTX) *Rooms {
	return &Rooms{q: db.New(database)}
}

var _ RoomStore = (*Rooms)(nil)

type Room struct {
	ID        int64
	Name      string
	X, Y, W, H float64
	SortOrder int64
}

type WriteRoom struct {
	Name      string
	X, Y, W, H float64
	SortOrder int64
}

func (r *Rooms) List(ctx context.Context) ([]Room, error) {
	rows, err := r.q.ListRooms(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]Room, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapRoom(row))
	}
	return out, nil
}

func (r *Rooms) Get(ctx context.Context, id int64) (Room, error) {
	row, err := r.q.GetRoom(ctx, id)
	if err != nil {
		return Room{}, mapDBErr(err)
	}
	return mapRoom(row), nil
}

func (r *Rooms) Create(ctx context.Context, in WriteRoom) (Room, error) {
	name := strings.TrimSpace(in.Name)
	if name == "" {
		return Room{}, apperr.ErrBadRequest
	}
	w, h := in.W, in.H
	if w <= 0 {
		w = 48
	}
	if h <= 0 {
		h = 40
	}
	id, err := r.q.CreateRoom(ctx, db.CreateRoomParams{
		Name: name, X: in.X, Y: in.Y, W: w, H: h, SortOrder: in.SortOrder,
	})
	if err != nil {
		return Room{}, mapDBErr(err)
	}
	return r.Get(ctx, id)
}

func (r *Rooms) Update(ctx context.Context, id int64, in WriteRoom) (Room, error) {
	name := strings.TrimSpace(in.Name)
	if name == "" {
		return Room{}, apperr.ErrBadRequest
	}
	w, h := in.W, in.H
	if w <= 0 {
		w = 48
	}
	if h <= 0 {
		h = 40
	}
	n, err := r.q.UpdateRoom(ctx, db.UpdateRoomParams{
		Name: name, X: in.X, Y: in.Y, W: w, H: h, SortOrder: in.SortOrder, ID: id,
	})
	if err != nil {
		return Room{}, mapDBErr(err)
	}
	if n == 0 {
		return Room{}, apperr.ErrNotFound
	}
	return r.Get(ctx, id)
}

func (r *Rooms) Delete(ctx context.Context, id int64) error {
	n, err := r.q.DeleteRoom(ctx, id)
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return apperr.ErrNotFound
	}
	return nil
}

func mapRoom(row db.Room) Room {
	return Room{
		ID: row.ID, Name: row.Name,
		X: row.X, Y: row.Y, W: row.W, H: row.H,
		SortOrder: row.SortOrder,
	}
}

func (r Room) ToDTO() dto.Room {
	return dto.Room{
		ID: r.ID, Name: r.Name,
		X: r.X, Y: r.Y, W: r.W, H: r.H,
		SortOrder: r.SortOrder,
	}
}
