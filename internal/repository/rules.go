package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/db"
	apperr "github.com/bartwork/home/pkg/errors"
)

type RuleStore interface {
	List(ctx context.Context) ([]Rule, error)
	Get(ctx context.Context, id int64) (Rule, error)
	Create(ctx context.Context, in WriteRule) (Rule, error)
	Update(ctx context.Context, id int64, in WriteRule) (Rule, error)
	SetEnabled(ctx context.Context, id int64, enabled bool) (Rule, error)
	Delete(ctx context.Context, id int64) error
}

type Rules struct {
	q *db.Queries
}

func NewRules(database db.DBTX) *Rules {
	return &Rules{q: db.New(database)}
}

var _ RuleStore = (*Rules)(nil)

type Rule struct {
	ID         int64
	Name       string
	Enabled    bool
	Mode       string
	CooldownMs int64
	GraphJSON  string
	CreatedAt  string
	UpdatedAt  string
}

type WriteRule struct {
	Name       string
	Enabled    bool
	Mode       string
	CooldownMs int64
	GraphJSON  string
}

func (r Rule) ToDTO() (dto.Rule, error) {
	graph := json.RawMessage(strings.TrimSpace(r.GraphJSON))
	if len(graph) == 0 {
		graph = json.RawMessage(`{"nodes":[],"edges":[]}`)
	}
	if !json.Valid(graph) {
		return dto.Rule{}, apperr.ErrBadRequest
	}
	return dto.Rule{
		ID: r.ID, Name: r.Name, Enabled: r.Enabled, Mode: r.Mode,
		CooldownMs: r.CooldownMs, Graph: graph,
		CreatedAt: r.CreatedAt, UpdatedAt: r.UpdatedAt,
	}, nil
}

func (r *Rules) List(ctx context.Context) ([]Rule, error) {
	rows, err := r.q.ListRules(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]Rule, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapRule(row))
	}
	return out, nil
}

func (r *Rules) Get(ctx context.Context, id int64) (Rule, error) {
	row, err := r.q.GetRule(ctx, id)
	if err != nil {
		return Rule{}, mapDBErr(err)
	}
	return mapRule(row), nil
}

func (r *Rules) Create(ctx context.Context, in WriteRule) (Rule, error) {
	mode := normalizeMode(in.Mode)
	id, err := r.q.CreateRule(ctx, db.CreateRuleParams{
		Name: strings.TrimSpace(in.Name), Enabled: in.Enabled, Mode: mode,
		CooldownMs: in.CooldownMs, GraphJson: in.GraphJSON,
	})
	if err != nil {
		return Rule{}, mapDBErr(err)
	}
	return r.Get(ctx, id)
}

func (r *Rules) Update(ctx context.Context, id int64, in WriteRule) (Rule, error) {
	mode := normalizeMode(in.Mode)
	n, err := r.q.UpdateRule(ctx, db.UpdateRuleParams{
		Name: strings.TrimSpace(in.Name), Enabled: in.Enabled, Mode: mode,
		CooldownMs: in.CooldownMs, GraphJson: in.GraphJSON, ID: id,
	})
	if err != nil {
		return Rule{}, mapDBErr(err)
	}
	if n == 0 {
		return Rule{}, mapDBErr(sql.ErrNoRows)
	}
	return r.Get(ctx, id)
}

func (r *Rules) SetEnabled(ctx context.Context, id int64, enabled bool) (Rule, error) {
	n, err := r.q.SetRuleEnabled(ctx, db.SetRuleEnabledParams{Enabled: enabled, ID: id})
	if err != nil {
		return Rule{}, mapDBErr(err)
	}
	if n == 0 {
		return Rule{}, mapDBErr(sql.ErrNoRows)
	}
	return r.Get(ctx, id)
}

func (r *Rules) Delete(ctx context.Context, id int64) error {
	n, err := r.q.DeleteRule(ctx, id)
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return mapDBErr(sql.ErrNoRows)
	}
	return nil
}

func mapRule(row db.Rule) Rule {
	return Rule{
		ID: row.ID, Name: row.Name, Enabled: row.Enabled, Mode: row.Mode,
		CooldownMs: row.CooldownMs, GraphJSON: row.GraphJson,
		CreatedAt: row.CreatedAt, UpdatedAt: row.UpdatedAt,
	}
}

func normalizeMode(mode string) string {
	switch strings.ToLower(strings.TrimSpace(mode)) {
	case "restart":
		return "restart"
	case "queued":
		return "queued"
	default:
		return "single"
	}
}
