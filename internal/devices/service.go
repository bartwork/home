package devices

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
)

type Service struct {
	repo repository.DeviceStore
}

func New(repo repository.DeviceStore) *Service {
	return &Service{repo: repo}
}

func (s *Service) ListDevices(ctx context.Context) ([]dto.Device, error) {
	rows, err := s.repo.List(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]dto.Device, 0, len(rows))
	for _, row := range rows {
		out = append(out, toDTO(row))
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
	return toDTO(row), nil
}

func (s *Service) CreateDevice(ctx context.Context, name, mqttTopic, deviceType, unit string, active bool) (dto.Device, error) {
	name = strings.TrimSpace(name)
	mqttTopic = repository.NormalizeTopic(mqttTopic)
	deviceType = strings.TrimSpace(deviceType)
	if name == "" || mqttTopic == "" || deviceType == "" {
		return dto.Device{}, errors.ErrBadRequest
	}
	row, err := s.repo.Create(ctx, repository.WriteDevice{
		Name: name, MqttTopic: mqttTopic, DeviceType: deviceType, Unit: strings.TrimSpace(unit), Active: active,
	})
	if err != nil {
		return dto.Device{}, err
	}
	return toDTO(row), nil
}

func (s *Service) UpdateDevice(ctx context.Context, id int64, name, mqttTopic, deviceType, unit string, active bool) (dto.Device, error) {
	if id <= 0 {
		return dto.Device{}, errors.ErrBadRequest
	}
	name = strings.TrimSpace(name)
	mqttTopic = repository.NormalizeTopic(mqttTopic)
	deviceType = strings.TrimSpace(deviceType)
	if name == "" || mqttTopic == "" || deviceType == "" {
		return dto.Device{}, errors.ErrBadRequest
	}
	row, err := s.repo.Update(ctx, id, repository.WriteDevice{
		Name: name, MqttTopic: mqttTopic, DeviceType: deviceType, Unit: strings.TrimSpace(unit), Active: active,
	})
	if err != nil {
		return dto.Device{}, err
	}
	return toDTO(row), nil
}

func (s *Service) DeleteDevice(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	return s.repo.Delete(ctx, id)
}

func toDTO(d repository.Device) dto.Device {
	return dto.Device{
		ID: d.ID, Name: d.Name, MqttTopic: d.MqttTopic, DeviceType: d.DeviceType,
		Unit: d.Unit, Active: d.Active, CreatedAt: d.CreatedAt,
	}
}
