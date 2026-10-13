package discovery

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
)

type Service struct {
	disco repository.DiscoveredStore
	devs  repository.DeviceStore
}

func NewService(disco repository.DiscoveredStore, devs repository.DeviceStore) *Service {
	return &Service{disco: disco, devs: devs}
}

func (s *Service) ListNew(ctx context.Context) ([]dto.Discovered, error) {
	rows, err := s.disco.ListNew(ctx)
	if err != nil {
		return nil, err
	}
	return mapList(rows), nil
}

func (s *Service) CountNew(ctx context.Context) (int64, error) {
	return s.disco.CountNew(ctx)
}

func (s *Service) Accept(ctx context.Context, id int64) (dto.Device, error) {
	if id <= 0 {
		return dto.Device{}, errors.ErrBadRequest
	}
	row, err := s.disco.Get(ctx, id)
	if err != nil {
		return dto.Device{}, err
	}
	if row.Status != "new" && row.Status != "ignored" {
		if row.Status == "accepted" {
			dev, err := s.devs.GetByTopic(ctx, row.MqttTopic)
			if err != nil {
				return dto.Device{}, err
			}
			return deviceDTO(dev), nil
		}
		return dto.Device{}, errors.ErrBadRequest
	}

	typ := row.SuggestedType
	if typ == "" {
		typ = "raw"
	}
	name := row.Title
	if name == "" {
		name = DefaultTitle(row.WbDevice, row.WbControl)
	}

	dev, err := s.devs.Create(ctx, repository.WriteDevice{
		Name:       name,
		MqttTopic:  row.MqttTopic,
		DeviceType: typ,
		Unit:       row.Unit,
		Active:     true,
		WbDevice:   row.WbDevice,
		WbControl:  row.WbControl,
		Source:     "discovery",
	})
	if err != nil {
		if err == errors.ErrConflict {
			existing, gerr := s.devs.GetByTopic(ctx, row.MqttTopic)
			if gerr != nil {
				return dto.Device{}, err
			}
			_ = s.disco.SetStatus(ctx, id, "accepted")
			return deviceDTO(existing), nil
		}
		return dto.Device{}, err
	}
	if err := s.disco.SetStatus(ctx, id, "accepted"); err != nil {
		return dto.Device{}, err
	}
	return deviceDTO(dev), nil
}

func (s *Service) Ignore(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	row, err := s.disco.Get(ctx, id)
	if err != nil {
		return err
	}
	if row.Status == "accepted" {
		return errors.ErrBadRequest
	}
	return s.disco.SetStatus(ctx, id, "ignored")
}

func (s *Service) AcceptAll(ctx context.Context, typ string) (accepted int, err error) {
	typ = strings.TrimSpace(typ)
	var rows []repository.Discovered
	if typ == "" {
		rows, err = s.disco.ListNew(ctx)
	} else {
		rows, err = s.disco.ListNewByType(ctx, typ)
	}
	if err != nil {
		return 0, err
	}
	for _, row := range rows {
		if typ == "" && (row.SuggestedType == "" || row.SuggestedType == "raw") {
			continue
		}
		if _, err := s.Accept(ctx, row.ID); err != nil {
			continue
		}
		accepted++
	}
	return accepted, nil
}

func mapList(rows []repository.Discovered) []dto.Discovered {
	out := make([]dto.Discovered, 0, len(rows))
	for _, r := range rows {
		out = append(out, dto.Discovered{
			ID:            r.ID,
			MqttTopic:     r.MqttTopic,
			WbDevice:      r.WbDevice,
			WbControl:     r.WbControl,
			Title:         r.Title,
			SuggestedType: r.SuggestedType,
			Unit:          r.Unit,
			LastPayload:   r.LastPayload,
			LastSeenAt:    r.LastSeenAt,
			Status:        r.Status,
		})
	}
	return out
}

func deviceDTO(d repository.Device) dto.Device {
	return dto.Device{
		ID: d.ID, Name: d.Name, MqttTopic: d.MqttTopic, DeviceType: d.DeviceType,
		Unit: d.Unit, Active: d.Active, CreatedAt: d.CreatedAt,
		WbDevice: d.WbDevice, WbControl: d.WbControl, Source: d.Source,
	}
}
