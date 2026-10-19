package repository

import (
	"context"

	"github.com/bartwork/home/internal/db"
)

type TopicShadowStore interface {
	List(ctx context.Context) ([]TopicShadow, error)
	Upsert(ctx context.Context, topic, payload string) error
}

type TopicShadows struct {
	q *db.Queries
}

func NewTopicShadows(database db.DBTX) *TopicShadows {
	return &TopicShadows{q: db.New(database)}
}

var _ TopicShadowStore = (*TopicShadows)(nil)

type TopicShadow struct {
	Topic     string
	Payload   string
	UpdatedAt string
}

func (r *TopicShadows) List(ctx context.Context) ([]TopicShadow, error) {
	rows, err := r.q.ListTopicShadow(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]TopicShadow, 0, len(rows))
	for _, row := range rows {
		out = append(out, TopicShadow{Topic: row.Topic, Payload: row.Payload, UpdatedAt: row.UpdatedAt})
	}
	return out, nil
}

func (r *TopicShadows) Upsert(ctx context.Context, topic, payload string) error {
	return r.q.UpsertTopicShadow(ctx, db.UpsertTopicShadowParams{Topic: topic, Payload: payload})
}
