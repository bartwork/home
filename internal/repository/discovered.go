package repository

import (
	"context"
	"time"

	"github.com/bartwork/home/internal/db"
	apperr "github.com/bartwork/home/pkg/errors"
)

type DiscoveredStore interface {
	ListNew(ctx context.Context) ([]Discovered, error)
	ListNewByType(ctx context.Context, typ string) ([]Discovered, error)
	Get(ctx context.Context, id int64) (Discovered, error)
	UpsertState(ctx context.Context, in WriteDiscovered) error
	UpsertMeta(ctx context.Context, in WriteDiscovered) error
	SetStatus(ctx context.Context, id int64, status string) error
	CountNew(ctx context.Context) (int64, error)
}

type DiscoveredRepo struct {
	q *db.Queries
}

func NewDiscovered(database db.DBTX) *DiscoveredRepo {
	return &DiscoveredRepo{q: db.New(database)}
}

var _ DiscoveredStore = (*DiscoveredRepo)(nil)

type Discovered struct {
	ID            int64
	MqttTopic     string
	WbDevice      string
	WbControl     string
	Title         string
	SuggestedType string
	Unit          string
	LastPayload   string
	LastSeenAt    string
	Status        string
	CreatedAt     string
}

type WriteDiscovered struct {
	MqttTopic     string
	WbDevice      string
	WbControl     string
	Title         string
	SuggestedType string
	Unit          string
	LastPayload   string
}

func (r *DiscoveredRepo) ListNew(ctx context.Context) ([]Discovered, error) {
	rows, err := r.q.ListDiscoveredNew(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]Discovered, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapDiscovered(row))
	}
	return out, nil
}

func (r *DiscoveredRepo) ListNewByType(ctx context.Context, typ string) ([]Discovered, error) {
	rows, err := r.q.ListDiscoveredNewByType(ctx, typ)
	if err != nil {
		return nil, err
	}
	out := make([]Discovered, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapDiscovered(row))
	}
	return out, nil
}

func (r *DiscoveredRepo) Get(ctx context.Context, id int64) (Discovered, error) {
	row, err := r.q.GetDiscovered(ctx, id)
	if err != nil {
		return Discovered{}, mapDBErr(err)
	}
	return mapDiscovered(row), nil
}

func (r *DiscoveredRepo) UpsertState(ctx context.Context, in WriteDiscovered) error {
	now := time.Now().UTC().Format(time.RFC3339)
	return mapDBErr(r.q.UpsertDiscoveredState(ctx, db.UpsertDiscoveredStateParams{
		MqttTopic:     in.MqttTopic,
		WbDevice:      in.WbDevice,
		WbControl:     in.WbControl,
		Title:         in.Title,
		SuggestedType: in.SuggestedType,
		Unit:          in.Unit,
		LastPayload:   in.LastPayload,
		LastSeenAt:    now,
	}))
}

func (r *DiscoveredRepo) UpsertMeta(ctx context.Context, in WriteDiscovered) error {
	now := time.Now().UTC().Format(time.RFC3339)
	return mapDBErr(r.q.UpsertDiscoveredMeta(ctx, db.UpsertDiscoveredMetaParams{
		MqttTopic:     in.MqttTopic,
		WbDevice:      in.WbDevice,
		WbControl:     in.WbControl,
		Title:         in.Title,
		SuggestedType: in.SuggestedType,
		Unit:          in.Unit,
		LastSeenAt:    now,
	}))
}

func (r *DiscoveredRepo) SetStatus(ctx context.Context, id int64, status string) error {
	n, err := r.q.SetDiscoveredStatus(ctx, db.SetDiscoveredStatusParams{
		Status: status,
		ID:     id,
	})
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return apperr.ErrNotFound
	}
	return nil
}

func (r *DiscoveredRepo) CountNew(ctx context.Context) (int64, error) {
	return r.q.CountDiscoveredNew(ctx)
}

func mapDiscovered(row db.DiscoveredDevice) Discovered {
	return Discovered{
		ID:            row.ID,
		MqttTopic:     row.MqttTopic,
		WbDevice:      row.WbDevice,
		WbControl:     row.WbControl,
		Title:         row.Title,
		SuggestedType: row.SuggestedType,
		Unit:          row.Unit,
		LastPayload:   row.LastPayload,
		LastSeenAt:    row.LastSeenAt,
		Status:        row.Status,
		CreatedAt:     row.CreatedAt,
	}
}
