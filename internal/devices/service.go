package devices

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/layout"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
)

type Service struct {
	repo     repository.DeviceStore
	onChange func()
}

func New(repo repository.DeviceStore) *Service {
	return &Service{repo: repo}
}

// OnChange — после create/update/delete (например air.RefreshIndex).
func (s *Service) OnChange(fn func()) {
	s.onChange = fn
}

func (s *Service) notifyChange() {
	if s.onChange != nil {
		s.onChange()
	}
}

func (s *Service) ListDevices(ctx context.Context) ([]dto.Device, error) {
	rows, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]dto.Device, 0, len(rows))
	for _, row := range rows {
		out = append(out, row.ToDTO())
	}
	return out, nil
}

func (s *Service) GetDevice(ctx context.Context, id int64) (dto.Device, error) {
	if id <= 0 {
		return dto.Device{}, errors.ErrBadRequest
	}
	row, err := s.repo.Get(ctx, id)
	if err != nil {
		return dto.Device{}, err
	}
	return row.ToDTO(), nil
}

func (s *Service) CreateDevice(ctx context.Context, name, mqttTopic, deviceType, unit, powerTopic string, roomID int64, icon string, x, y, w, h float64, active bool) (dto.Device, error) {
	name = strings.TrimSpace(name)
	mqttTopic = repository.NormalizeTopic(mqttTopic)
	deviceType = strings.TrimSpace(deviceType)
	if name == "" || mqttTopic == "" || deviceType == "" {
		return dto.Device{}, errors.ErrBadRequest
	}
	row, err := s.repo.Create(ctx, repository.WriteDevice{
		Name: name, MqttTopic: mqttTopic, DeviceType: deviceType,
		Unit: strings.TrimSpace(unit), PowerTopic: resolvePower(mqttTopic, powerTopic, deviceType, true),
		RoomID: roomPtr(roomID), Icon: strings.TrimSpace(icon),
		X: x, Y: y, W: w, H: h, Active: active,
	})
	if err != nil {
		return dto.Device{}, err
	}
	s.notifyChange()
	return row.ToDTO(), nil
}

func (s *Service) UpdateDevice(ctx context.Context, id int64, name, mqttTopic, deviceType, unit, powerTopic string, roomID int64, icon string, x, y, w, h float64, active bool) (dto.Device, error) {
	if id <= 0 {
		return dto.Device{}, errors.ErrBadRequest
	}
	name = strings.TrimSpace(name)
	mqttTopic = repository.NormalizeTopic(mqttTopic)
	deviceType = strings.TrimSpace(deviceType)
	if name == "" || mqttTopic == "" || deviceType == "" {
		return dto.Device{}, errors.ErrBadRequest
	}

	cur, err := s.repo.Get(ctx, id)
	if err != nil {
		return dto.Device{}, err
	}

	// Update: пустой powerTopic сохраняем как есть (можно сбросить).
	row, err := s.repo.Update(ctx, id, repository.WriteDevice{
		Name: name, MqttTopic: mqttTopic, DeviceType: deviceType,
		Unit: strings.TrimSpace(unit), PowerTopic: strings.TrimSpace(powerTopic),
		RoomID: roomPtr(roomID), Icon: strings.TrimSpace(icon),
		X: x, Y: y, W: w, H: h, Active: active,
		WbDevice: cur.WbDevice, WbControl: cur.WbControl, Source: cur.Source,
	})
	if err != nil {
		return dto.Device{}, err
	}
	s.notifyChange()
	return row.ToDTO(), nil
}

func (s *Service) DeleteDevice(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	if err := s.repo.Delete(ctx, id); err != nil {
		return err
	}
	s.notifyChange()
	return nil
}

func resolvePower(mqttTopic, powerTopic, deviceType string, infer bool) string {
	powerTopic = strings.TrimSpace(powerTopic)
	if powerTopic != "" {
		return powerTopic
	}
	if infer && deviceType == "range" {
		return layout.InferPowerTopic(mqttTopic)
	}
	return ""
}

func roomPtr(id int64) *int64 {
	if id <= 0 {
		return nil
	}
	return &id
}
