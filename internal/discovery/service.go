package discovery

import (
	"context"
	"strings"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/layout"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
)

type Service struct {
	disco repository.DiscoveredStore
	devs  repository.DeviceStore
	rooms repository.RoomStore
}

func NewService(disco repository.DiscoveredStore, devs repository.DeviceStore, rooms repository.RoomStore) *Service {
	return &Service{disco: disco, devs: devs, rooms: rooms}
}

func usefulType(typ string) bool {
	switch typ {
	case "switch", "range", "sensor":
		return true
	default:
		return false
	}
}

func (s *Service) ListNew(ctx context.Context) ([]dto.Discovered, error) {
	rows, err := s.disco.ListNew(ctx)
	if err != nil {
		return nil, err
	}
	return mapList(rows), nil
}

func (s *Service) ListIgnored(ctx context.Context) ([]dto.Discovered, error) {
	rows, err := s.disco.ListIgnored(ctx)
	if err != nil {
		return nil, err
	}
	return mapList(rows), nil
}

func (s *Service) CountNew(ctx context.Context) (int64, error) {
	return s.disco.CountNew(ctx)
}

type AcceptOpts struct {
	Name   string
	RoomID int64
	Icon   string
}

func (s *Service) Accept(ctx context.Context, id int64, opts AcceptOpts) (dto.Device, error) {
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
			return dev.ToDTO(), nil
		}
		return dto.Device{}, errors.ErrBadRequest
	}

	typ := row.SuggestedType
	if typ == "" {
		typ = "raw"
	}
	name := strings.TrimSpace(opts.Name)
	if name == "" {
		name = row.Title
	}
	if name == "" {
		name = DefaultTitle(row.WbDevice, row.WbControl)
	}

	var roomID *int64
	if opts.RoomID > 0 {
		if s.rooms != nil {
			if _, err := s.rooms.Get(ctx, opts.RoomID); err != nil {
				return dto.Device{}, err
			}
		}
		roomID = &opts.RoomID
	}

	idx := 0
	if all, lerr := s.devs.List(ctx); lerr == nil {
		for _, d := range all {
			if roomID != nil && d.RoomID != nil && *d.RoomID == *roomID {
				idx++
			} else if roomID == nil && d.RoomID == nil {
				idx++
			}
		}
	}
	x, y, w, h := layout.Tile(idx, 3)

	power := ""
	if typ == "range" {
		power = layout.InferPowerTopic(row.MqttTopic)
	}
	dev, err := s.devs.Create(ctx, repository.WriteDevice{
		Name:       name,
		MqttTopic:  row.MqttTopic,
		DeviceType: typ,
		Unit:       row.Unit,
		PowerTopic: power,
		RoomID:     roomID,
		Icon:       layout.IconFor(strings.TrimSpace(opts.Icon), name, typ),
		X:          x,
		Y:          y,
		W:          w,
		H:          h,
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
			return existing.ToDTO(), nil
		}
		return dto.Device{}, err
	}
	if err := s.disco.SetStatus(ctx, id, "accepted"); err != nil {
		return dto.Device{}, err
	}
	return dev.ToDTO(), nil
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

// Restore возвращает скрытый кандидат в статус new.
func (s *Service) Restore(ctx context.Context, id int64) error {
	if id <= 0 {
		return errors.ErrBadRequest
	}
	row, err := s.disco.Get(ctx, id)
	if err != nil {
		return err
	}
	if row.Status != "ignored" {
		return errors.ErrBadRequest
	}
	return s.disco.SetStatus(ctx, id, "new")
}

// AcceptAll принимает полезные типы (switch/range/sensor).
// При ignoreRaw=true остальные new (raw/пусто) скрываются.
func (s *Service) AcceptAll(ctx context.Context, typ string, ignoreRaw bool) (accepted, ignored, failed int, err error) {
	typ = strings.TrimSpace(typ)
	var rows []repository.Discovered
	if typ == "" {
		rows, err = s.disco.ListNew(ctx)
	} else {
		rows, err = s.disco.ListNewByType(ctx, typ)
	}
	if err != nil {
		return 0, 0, 0, err
	}
	for _, row := range rows {
		if typ == "" && !usefulType(row.SuggestedType) {
			if ignoreRaw {
				if ierr := s.Ignore(ctx, row.ID); ierr == nil {
					ignored++
				} else {
					failed++
				}
			}
			continue
		}
		if _, aerr := s.Accept(ctx, row.ID, AcceptOpts{}); aerr != nil {
			failed++
			continue
		}
		accepted++
	}
	return accepted, ignored, failed, nil
}

func mapList(rows []repository.Discovered) []dto.Discovered {
	out := make([]dto.Discovered, 0, len(rows))
	for _, r := range rows {
		out = append(out, r.ToDTO())
	}
	return out
}
