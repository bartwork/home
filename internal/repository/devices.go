package repository

import (
	"context"
	"strings"

	"github.com/bartwork/home/internal/db"
	apperr "github.com/bartwork/home/pkg/errors"
)

type DeviceStore interface {
	List(ctx context.Context) ([]Device, error)
	Get(ctx context.Context, id int64) (Device, error)
	Create(ctx context.Context, in WriteDevice) (Device, error)
	Update(ctx context.Context, id int64, in WriteDevice) (Device, error)
	Delete(ctx context.Context, id int64) error
}

type Devices struct {
	q *db.Queries
}

func NewDevices(database db.DBTX) *Devices {
	return &Devices{q: db.New(database)}
}

var _ DeviceStore = (*Devices)(nil)

type Device struct {
	ID         int64
	Name       string
	MqttTopic  string
	DeviceType string
	Unit       string
	Active     bool
	CreatedAt  string
}

type WriteDevice struct {
	Name       string
	MqttTopic  string
	DeviceType string
	Unit       string
	Active     bool
}

func (r *Devices) List(ctx context.Context) ([]Device, error) {
	rows, err := r.q.ListDevices(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]Device, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapDevice(row))
	}
	return out, nil
}

func (r *Devices) Get(ctx context.Context, id int64) (Device, error) {
	row, err := r.q.GetDevice(ctx, id)
	if err != nil {
		return Device{}, mapDBErr(err)
	}
	return mapDevice(row), nil
}

func (r *Devices) Create(ctx context.Context, in WriteDevice) (Device, error) {
	id, err := r.q.CreateDevice(ctx, db.CreateDeviceParams{
		Name:       in.Name,
		MqttTopic:  in.MqttTopic,
		DeviceType: in.DeviceType,
		Unit:       in.Unit,
		IsActive:   in.Active,
	})
	if err != nil {
		return Device{}, mapDBErr(err)
	}
	return r.Get(ctx, id)
}

func (r *Devices) Update(ctx context.Context, id int64, in WriteDevice) (Device, error) {
	n, err := r.q.UpdateDevice(ctx, db.UpdateDeviceParams{
		Name:       in.Name,
		MqttTopic:  in.MqttTopic,
		DeviceType: in.DeviceType,
		Unit:       in.Unit,
		IsActive:   in.Active,
		ID:         id,
	})
	if err != nil {
		return Device{}, mapDBErr(err)
	}
	if n == 0 {
		return Device{}, apperr.ErrNotFound
	}
	return r.Get(ctx, id)
}

func (r *Devices) Delete(ctx context.Context, id int64) error {
	n, err := r.q.DeleteDevice(ctx, id)
	if err != nil {
		return mapDBErr(err)
	}
	if n == 0 {
		return apperr.ErrNotFound
	}
	return nil
}

func mapDevice(row db.Device) Device {
	return Device{
		ID:         row.ID,
		Name:       row.Name,
		MqttTopic:  row.MqttTopic,
		DeviceType: row.DeviceType,
		Unit:       row.Unit,
		Active:     row.IsActive,
		CreatedAt:  row.CreatedAt,
	}
}

func NormalizeTopic(topic string) string {
	return strings.TrimSpace(topic)
}
