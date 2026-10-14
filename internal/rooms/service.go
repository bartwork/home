package rooms

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/layout"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
)

type Service struct {
	repo repository.RoomStore
}

func New(repo repository.RoomStore) *Service {
	return &Service{repo: repo}
}

func (s *Service) List(ctx context.Context) ([]dto.Room, error) {
	rows, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]dto.Room, 0, len(rows))
	for _, r := range rows {
		out = append(out, r.ToDTO())
	}
	return out, nil
}

func (s *Service) Create(ctx context.Context, in dto.Room) (dto.Room, error) {
	name := strings.TrimSpace(in.Name)
	if name == "" {
		return dto.Room{}, errors.ErrBadRequest
	}
	sort := in.SortOrder
	if sort <= 0 {
		existing, err := s.repo.List(ctx)
		if err != nil {
			return dto.Room{}, err
		}
		var max int64
		for _, r := range existing {
			if r.SortOrder > max {
				max = r.SortOrder
			}
		}
		sort = max + 1
	}
	x, y, w, h := layout.ClampRect(in.X, in.Y, in.W, in.H)
	row, err := s.repo.Create(ctx, repository.WriteRoom{
		Name: name, X: x, Y: y, W: w, H: h, SortOrder: sort,
	})
	if err != nil {
		return dto.Room{}, err
	}
	return row.ToDTO(), nil
}

func (s *Service) Update(ctx context.Context, id int64, in dto.Room) (dto.Room, error) {
	if id <= 0 {
		return dto.Room{}, errors.ErrBadRequest
	}
	name := strings.TrimSpace(in.Name)
	if name == "" {
		return dto.Room{}, errors.ErrBadRequest
	}
	cur, err := s.repo.Get(ctx, id)
	if err != nil {
		return dto.Room{}, err
	}
	sort := in.SortOrder
	if sort <= 0 {
		sort = cur.SortOrder
	}
	x, y, w, h := layout.ClampRect(in.X, in.Y, in.W, in.H)
	row, err := s.repo.Update(ctx, id, repository.WriteRoom{
		Name: name, X: x, Y: y, W: w, H: h, SortOrder: sort,
	})
	if err != nil {
		return dto.Room{}, err
	}
	return row.ToDTO(), nil
}

func (s *Service) Delete(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	return s.repo.Delete(ctx, id)
}
