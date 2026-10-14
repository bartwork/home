package repository

import (
	"context"
	"strings"

	"github.com/bartwork/home/internal/db"
	"github.com/bartwork/home/internal/layout"
	apperr "github.com/bartwork/home/pkg/errors"
)

type DeviceStore interface {
	List(ctx context.Context) ([]Device, error)
	Get(ctx context.Context, id int64) (Device, error)
	GetByTopic(ctx context.Context, topic string) (Device, error)
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
	PowerTopic string
	RoomID     *int64
	RoomName   string
	Icon       string
	X, Y, W, H float64
	Active     bool
	CreatedAt  string
	WbDevice   string
	WbControl  string
	Source     string
}

type WriteDevice struct {
	Name       string
	MqttTopic  string
	DeviceType string
	Unit       string
	PowerTopic string
	RoomID     *int64
	Icon       string
	X, Y, W, H float64
	Active     bool
	WbDevice   string
	WbControl  string
	Source     string
}

func (r *Devices) List(ctx context.Context) ([]Device, error) {
	rows, err := r.q.ListDevices(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]Device, 0, len(rows))
	for _, row := range rows {
		out = append(out, mapListDevice(row))
	}
	return out, nil
}

func (r *Devices) Get(ctx context.Context, id int64) (Device, error) {
	row, err := r.q.GetDevice(ctx, id)
	if err != nil {
		return Device{}, mapDBErr(err)
	}
	return mapGetDevice(row), nil
}

func (r *Devices) GetByTopic(ctx context.Context, topic string) (Device, error) {
	row, err := r.q.GetDeviceByTopic(ctx, topic)
	if err != nil {
		return Device{}, mapDBErr(err)
	}
	return mapGetByTopic(row), nil
}

func (r *Devices) Create(ctx context.Context, in WriteDevice) (Device, error) {
	source := in.Source
	if source == "" {
		source = "manual"
	}
	x, y, w, h := layoutOrDefault(in.X, in.Y, in.W, in.H)
	id, err := r.q.CreateDevice(ctx, db.CreateDeviceParams{
		Name: in.Name, MqttTopic: in.MqttTopic, DeviceType: in.DeviceType, Unit: in.Unit,
		PowerTopic: strings.TrimSpace(in.PowerTopic),
		RoomID: in.RoomID, Icon: in.Icon, X: x, Y: y, W: w, H: h,
		IsActive: in.Active, WbDevice: in.WbDevice, WbControl: in.WbControl, Source: source,
	})
	if err != nil {
		return Device{}, mapDBErr(err)
	}
	return r.Get(ctx, id)
}

func (r *Devices) Update(ctx context.Context, id int64, in WriteDevice) (Device, error) {
	x, y, w, h := layoutOrDefault(in.X, in.Y, in.W, in.H)
	n, err := r.q.UpdateDevice(ctx, db.UpdateDeviceParams{
		Name: in.Name, MqttTopic: in.MqttTopic, DeviceType: in.DeviceType, Unit: in.Unit,
		PowerTopic: strings.TrimSpace(in.PowerTopic),
		RoomID: in.RoomID, Icon: in.Icon, X: x, Y: y, W: w, H: h,
		IsActive: in.Active, WbDevice: in.WbDevice, WbControl: in.WbControl, ID: id,
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

func layoutOrDefault(x, y, w, h float64) (float64, float64, float64, float64) {
	if w <= 0 {
		w = 44
	}
	if h <= 0 {
		h = 22
	}
	return layout.ClampRect(x, y, w, h)
}

func mapListDevice(row db.ListDevicesRow) Device {
	return Device{
		ID: row.ID, Name: row.Name, MqttTopic: row.MqttTopic, DeviceType: row.DeviceType,
		Unit: row.Unit, PowerTopic: row.PowerTopic, RoomID: row.RoomID, RoomName: row.RoomName,
		Icon: row.Icon, X: row.X, Y: row.Y, W: row.W, H: row.H, Active: row.IsActive,
		CreatedAt: row.CreatedAt, WbDevice: row.WbDevice, WbControl: row.WbControl, Source: row.Source,
	}
}

func mapGetDevice(row db.GetDeviceRow) Device {
	return Device{
		ID: row.ID, Name: row.Name, MqttTopic: row.MqttTopic, DeviceType: row.DeviceType,
		Unit: row.Unit, PowerTopic: row.PowerTopic, RoomID: row.RoomID, RoomName: row.RoomName,
		Icon: row.Icon, X: row.X, Y: row.Y, W: row.W, H: row.H, Active: row.IsActive,
		CreatedAt: row.CreatedAt, WbDevice: row.WbDevice, WbControl: row.WbControl, Source: row.Source,
	}
}

func mapGetByTopic(row db.GetDeviceByTopicRow) Device {
	return Device{
		ID: row.ID, Name: row.Name, MqttTopic: row.MqttTopic, DeviceType: row.DeviceType,
		Unit: row.Unit, PowerTopic: row.PowerTopic, RoomID: row.RoomID, RoomName: row.RoomName,
		Icon: row.Icon, X: row.X, Y: row.Y, W: row.W, H: row.H, Active: row.IsActive,
		CreatedAt: row.CreatedAt, WbDevice: row.WbDevice, WbControl: row.WbControl, Source: row.Source,
	}
}

func NormalizeTopic(topic string) string {
	return strings.TrimSpace(topic)
}
