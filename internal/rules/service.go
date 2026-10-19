package rules

import (
	"context"
	"encoding/json"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	apperr "github.com/bartwork/home/pkg/errors"
)

type Service struct {
	repo   repository.RuleStore
	engine *Engine
}

func NewService(repo repository.RuleStore, engine *Engine) *Service {
	return &Service{repo: repo, engine: engine}
}

func (s *Service) Engine() *Engine { return s.engine }

type WriteIn struct {
	Name       string          `json:"name"`
	Enabled    *bool           `json:"enabled"`
	Mode       string          `json:"mode"`
	CooldownMs int64           `json:"cooldownMs"`
	Graph      json.RawMessage `json:"graph"`
}

func (s *Service) List(ctx context.Context) ([]dto.Rule, error) {
	rows, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]dto.Rule, 0, len(rows))
	for _, row := range rows {
		d, err := row.ToDTO()
		if err != nil {
			return nil, err
		}
		out = append(out, d)
	}
	return out, nil
}

func (s *Service) Get(ctx context.Context, id int64) (dto.Rule, error) {
	row, err := s.repo.Get(ctx, id)
	if err != nil {
		return dto.Rule{}, err
	}
	return row.ToDTO()
}

func (s *Service) Create(ctx context.Context, in WriteIn) (dto.Rule, error) {
	wr, err := s.toWrite(in, repository.Rule{Enabled: true})
	if err != nil {
		return dto.Rule{}, err
	}
	row, err := s.repo.Create(ctx, wr)
	if err != nil {
		return dto.Rule{}, err
	}
	_ = s.engine.Reload(ctx)
	return row.ToDTO()
}

func (s *Service) Update(ctx context.Context, id int64, in WriteIn) (dto.Rule, error) {
	existing, err := s.repo.Get(ctx, id)
	if err != nil {
		return dto.Rule{}, err
	}
	wr, err := s.toWrite(in, existing)
	if err != nil {
		return dto.Rule{}, err
	}
	row, err := s.repo.Update(ctx, id, wr)
	if err != nil {
		return dto.Rule{}, err
	}
	_ = s.engine.Reload(ctx)
	return row.ToDTO()
}

func (s *Service) Delete(ctx context.Context, id int64) error {
	if err := s.repo.Delete(ctx, id); err != nil {
		return err
	}
	return s.engine.Reload(ctx)
}

func (s *Service) SetEnabled(ctx context.Context, id int64, enabled bool) (dto.Rule, error) {
	row, err := s.repo.SetEnabled(ctx, id, enabled)
	if err != nil {
		return dto.Rule{}, err
	}
	_ = s.engine.Reload(ctx)
	return row.ToDTO()
}

func (s *Service) Run(ctx context.Context, id int64) error {
	if _, err := s.repo.Get(ctx, id); err != nil {
		return err
	}
	return s.engine.RunManual(ctx, id)
}

func (s *Service) toWrite(in WriteIn, defaults repository.Rule) (repository.WriteRule, error) {
	name := strings.TrimSpace(in.Name)
	if name == "" {
		return repository.WriteRule{}, apperr.ErrBadRequest
	}
	if len(in.Graph) == 0 || !json.Valid(in.Graph) {
		return repository.WriteRule{}, apperr.ErrBadRequest
	}
	if err := ValidateGraphJSON(string(in.Graph)); err != nil {
		return repository.WriteRule{}, apperr.ErrBadRequest
	}
	enabled := defaults.Enabled
	if in.Enabled != nil {
		enabled = *in.Enabled
	}
	if in.CooldownMs < 0 {
		return repository.WriteRule{}, apperr.ErrBadRequest
	}
	return repository.WriteRule{
		Name: name, Enabled: enabled, Mode: in.Mode,
		CooldownMs: in.CooldownMs, GraphJSON: string(in.Graph),
	}, nil
}
