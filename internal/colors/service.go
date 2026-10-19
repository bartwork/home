package colors

import (
	"context"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
)

const MaxFavorites = 8

type Service struct {
	repo repository.ColorFavoriteStore
}

func New(repo repository.ColorFavoriteStore) *Service {
	return &Service{repo: repo}
}

func (s *Service) List(ctx context.Context) ([]dto.ColorFavorite, error) {
	rows, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}
	return toDTO(rows), nil
}

func (s *Service) seedDefaults(ctx context.Context) error {
	for i, c := range defaultPalette {
		if i >= MaxFavorites {
			break
		}
		if _, err := s.repo.Create(ctx, c.Hue, c.Saturation, c.Brightness, int64(i)); err != nil {
			return err
		}
	}
	return nil
}

// Reset заменяет всё стандартной палитрой.
func (s *Service) Reset(ctx context.Context) ([]dto.ColorFavorite, error) {
	if err := s.repo.DeleteAll(ctx); err != nil {
		return nil, err
	}
	if err := s.seedDefaults(ctx); err != nil {
		return nil, err
	}
	return s.List(ctx)
}

type CreateIn struct {
	Hue        int64 `json:"hue"`
	Saturation int64 `json:"saturation"`
	Brightness int64 `json:"brightness"`
}

func (s *Service) Create(ctx context.Context, in CreateIn) (dto.ColorFavorite, error) {
	if !validHSV(in.Hue, in.Saturation, in.Brightness) {
		return dto.ColorFavorite{}, errors.ErrBadRequest
	}
	rows, err := s.repo.List(ctx)
	if err != nil {
		return dto.ColorFavorite{}, err
	}
	for _, r := range rows {
		if r.Hue == in.Hue && r.Saturation == in.Saturation && r.Brightness == in.Brightness {
			return r.ToDTO(), nil
		}
	}
	sortOrder := nextSortOrder(rows)
	if len(rows) >= MaxFavorites {
		if err := s.repo.Delete(ctx, rows[0].ID); err != nil {
			return dto.ColorFavorite{}, err
		}
		rows = rows[1:]
		sortOrder = nextSortOrder(rows)
	}
	row, err := s.repo.Create(ctx, in.Hue, in.Saturation, in.Brightness, sortOrder)
	if err != nil {
		return dto.ColorFavorite{}, err
	}
	return row.ToDTO(), nil
}

func (s *Service) Delete(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	return s.repo.Delete(ctx, id)
}

func nextSortOrder(rows []repository.ColorFavorite) int64 {
	var max int64 = -1
	for _, r := range rows {
		if r.SortOrder > max {
			max = r.SortOrder
		}
	}
	return max + 1
}

func toDTO(rows []repository.ColorFavorite) []dto.ColorFavorite {
	out := make([]dto.ColorFavorite, 0, len(rows))
	for _, r := range rows {
		out = append(out, r.ToDTO())
	}
	return out
}

func validHSV(h, s, v int64) bool {
	return h >= 0 && h <= 360 && s >= 0 && s <= 100 && v >= 0 && v <= 100
}
