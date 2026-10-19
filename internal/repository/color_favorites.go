package repository

import (
	"context"
	"database/sql"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/db"
)

type ColorFavoriteStore interface {
	List(ctx context.Context) ([]ColorFavorite, error)
	Get(ctx context.Context, id int64) (ColorFavorite, error)
	Count(ctx context.Context) (int64, error)
	Create(ctx context.Context, hue, saturation, brightness, sortOrder int64) (ColorFavorite, error)
	Delete(ctx context.Context, id int64) error
	DeleteAll(ctx context.Context) error
}

type ColorFavorites struct {
	q *db.Queries
}

func NewColorFavorites(database db.DBTX) *ColorFavorites {
	return &ColorFavorites{q: db.New(database)}
}

var _ ColorFavoriteStore = (*ColorFavorites)(nil)

type ColorFavorite struct {
	ID         int64
	Hue        int64
	Saturation int64
	Brightness int64
	SortOrder  int64
	CreatedAt  string
}

func (c ColorFavorite) ToDTO() dto.ColorFavorite {
	return dto.ColorFavorite{
		ID: c.ID, Hue: c.Hue, Saturation: c.Saturation, Brightness: c.Brightness,
		SortOrder: c.SortOrder, CreatedAt: c.CreatedAt,
	}
}

func (r *ColorFavorites) List(ctx context.Context) ([]ColorFavorite, error) {
	rows, err := r.q.ListColorFavorites(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]ColorFavorite, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapColorFavorite(row))
	}
	return out, nil
}

func (r *ColorFavorites) Get(ctx context.Context, id int64) (ColorFavorite, error) {
	row, err := r.q.GetColorFavorite(ctx, id)
	if err != nil {
		return ColorFavorite{}, mapDBErr(err)
	}
	return mapColorFavorite(row), nil
}

func (r *ColorFavorites) Count(ctx context.Context) (int64, error) {
	return r.q.CountColorFavorites(ctx)
}

func (r *ColorFavorites) Create(ctx context.Context, hue, saturation, brightness, sortOrder int64) (ColorFavorite, error) {
	id, err := r.q.CreateColorFavorite(ctx, db.CreateColorFavoriteParams{
		Hue: hue, Saturation: saturation, Brightness: brightness, SortOrder: sortOrder,
	})
	if err != nil {
		return ColorFavorite{}, mapDBErr(err)
	}
	return r.Get(ctx, id)
}

func (r *ColorFavorites) Delete(ctx context.Context, id int64) error {
	n, err := r.q.DeleteColorFavorite(ctx, id)
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return mapDBErr(sql.ErrNoRows)
	}
	return nil
}

func (r *ColorFavorites) DeleteAll(ctx context.Context) error {
	return mapDBErr(r.q.DeleteAllColorFavorites(ctx))
}

func mapColorFavorite(row db.ColorFavorite) ColorFavorite {
	return ColorFavorite{
		ID: row.ID, Hue: row.Hue, Saturation: row.Saturation, Brightness: row.Brightness,
		SortOrder: row.SortOrder, CreatedAt: row.CreatedAt,
	}
}
