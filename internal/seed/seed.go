package seed

import (
	"context"
	"strings"

	"github.com/bartwork/home/internal/layout"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/pkg/errors"
	"github.com/rs/zerolog"
)

// Run дополняет каталог комнат и устройств (по имени / топику). Повторный запуск безопасен.
func Run(ctx context.Context, log zerolog.Logger, rooms repository.RoomStore, devices repository.DeviceStore) error {
	if err := seedRooms(ctx, log, rooms); err != nil {
		return err
	}
	if err := seedDevices(ctx, log, rooms, devices); err != nil {
		return err
	}
	if err := backfillPower(ctx, log, devices); err != nil {
		return err
	}
	if err := backfillFloor(ctx, log, devices); err != nil {
		return err
	}
	return backfillAir(ctx, log, devices)
}

func seedRooms(ctx context.Context, log zerolog.Logger, rooms repository.RoomStore) error {
	existing, err := rooms.List(ctx)
	if err != nil {
		return err
	}
	have := map[string]struct{}{}
	for _, r := range existing {
		have[r.Name] = struct{}{}
	}
	created := 0
	for _, r := range Rooms {
		if _, ok := have[r.Name]; ok {
			continue
		}
		if _, err := rooms.Create(ctx, repository.WriteRoom{
			Name: r.Name, X: r.X, Y: r.Y, W: r.W, H: r.H, SortOrder: r.SortOrder,
		}); err != nil {
			if err == errors.ErrConflict {
				continue
			}
			return err
		}
		created++
	}
	if created > 0 {
		log.Info().Int("rooms", created).Msg("seed rooms")
	}
	return nil
}

func seedDevices(ctx context.Context, log zerolog.Logger, rooms repository.RoomStore, devices repository.DeviceStore) error {
	existing, err := devices.List(ctx)
	if err != nil {
		return err
	}
	haveTopic := map[string]struct{}{}
	perRoom := map[string]int{}
	for _, d := range existing {
		haveTopic[d.MqttTopic] = struct{}{}
		key := ""
		if d.RoomID != nil {
			key = d.RoomName
		}
		perRoom[key]++
	}

	roomByName := map[string]int64{}
	allRooms, err := rooms.List(ctx)
	if err != nil {
		return err
	}
	for _, r := range allRooms {
		roomByName[r.Name] = r.ID
	}

	created := 0
	for _, d := range Devices {
		topic := strings.TrimSpace(d.Topic)
		if topic == "" || strings.Contains(topic, "?") {
			log.Warn().Str("name", d.Name).Str("topic", topic).Msg("seed skip incomplete topic")
			continue
		}
		if _, ok := haveTopic[topic]; ok {
			continue
		}
		if d.Type == "" {
			d.Type = "raw"
		}

		var roomID *int64
		if d.Room != "" {
			if id, ok := roomByName[d.Room]; ok {
				roomID = &id
			} else {
				log.Warn().Str("name", d.Name).Str("room", d.Room).Msg("seed room missing")
			}
		}

		idx := perRoom[d.Room]
		perRoom[d.Room] = idx + 1
		x, y, w, h := layout.Tile(idx, 3)

		wbDev, wbCtrl := "", ""
		if id, ctrl, ok := layout.SplitWBTopic(topic); ok {
			wbDev, wbCtrl = id, ctrl
		}

		_, err := devices.Create(ctx, repository.WriteDevice{
			Name:       d.Name,
			MqttTopic:  topic,
			DeviceType: d.Type,
			Unit:       d.Unit,
			PowerTopic: powerFor(d),
			RoomID:     roomID,
			Icon:       layout.IconFor(d.Icon, d.Name, d.Type),
			X:          x,
			Y:          y,
			W:          w,
			H:          h,
			Active:     true,
			WbDevice:   wbDev,
			WbControl:  wbCtrl,
			Source:     "seed",
		})
		if err != nil {
			if err == errors.ErrConflict {
				continue
			}
			return err
		}
		haveTopic[topic] = struct{}{}
		created++
	}
	if created > 0 {
		log.Info().Int("devices", created).Int("rooms", len(allRooms)).Msg("seed devices")
	}
	return nil
}

// backfillPower заполняет пустой power_topic у range (каталог или Infer).
func backfillPower(ctx context.Context, log zerolog.Logger, devices repository.DeviceStore) error {
	byTopic := map[string]DeviceDef{}
	for _, d := range Devices {
		byTopic[d.Topic] = d
	}
	existing, err := devices.List(ctx)
	if err != nil {
		return err
	}
	n := 0
	for _, d := range existing {
		if d.DeviceType != "range" || strings.TrimSpace(d.PowerTopic) != "" {
			continue
		}
		power := ""
		if def, ok := byTopic[d.MqttTopic]; ok {
			power = powerFor(def)
		} else {
			power = layout.InferPowerTopic(d.MqttTopic)
		}
		if power == "" {
			continue
		}
		if _, err := devices.Update(ctx, d.ID, repository.WriteDevice{
			Name: d.Name, MqttTopic: d.MqttTopic, DeviceType: d.DeviceType, Unit: d.Unit,
			PowerTopic: power, RoomID: d.RoomID, Icon: d.Icon,
			X: d.X, Y: d.Y, W: d.W, H: d.H, Active: d.Active,
			WbDevice: d.WbDevice, WbControl: d.WbControl, Source: d.Source,
		}); err != nil {
			return err
		}
		n++
	}
	if n > 0 {
		log.Info().Int("devices", n).Msg("seed backfill power_topic")
	}
	return nil
}

// backfillFloor переводит каталожные тёплые полы switch→floor и проставляет датчик.
func backfillFloor(ctx context.Context, log zerolog.Logger, devices repository.DeviceStore) error {
	byTopic := map[string]DeviceDef{}
	for _, d := range Devices {
		if d.Type == "floor" {
			byTopic[d.Topic] = d
		}
	}
	existing, err := devices.List(ctx)
	if err != nil {
		return err
	}
	n := 0
	for _, d := range existing {
		def, ok := byTopic[d.MqttTopic]
		if !ok {
			continue
		}
		sensor := powerFor(def)
		if d.DeviceType == "floor" && strings.TrimSpace(d.PowerTopic) == sensor {
			continue
		}
		icon := d.Icon
		if icon == "" {
			icon = layout.IconFor(def.Icon, def.Name, "floor")
		}
		if _, err := devices.Update(ctx, d.ID, repository.WriteDevice{
			Name: d.Name, MqttTopic: d.MqttTopic, DeviceType: "floor", Unit: d.Unit,
			PowerTopic: sensor, RoomID: d.RoomID, Icon: icon,
			X: d.X, Y: d.Y, W: d.W, H: d.H, Active: d.Active,
			WbDevice: d.WbDevice, WbControl: d.WbControl, Source: d.Source,
		}); err != nil {
			return err
		}
		n++
	}
	if n > 0 {
		log.Info().Int("devices", n).Msg("seed backfill floor")
	}
	return nil
}

// backfillAir приводит каталожные MSW к типу air и имени из seed.
func backfillAir(ctx context.Context, log zerolog.Logger, devices repository.DeviceStore) error {
	byTopic := map[string]DeviceDef{}
	for _, d := range Devices {
		if d.Type == "air" {
			byTopic[d.Topic] = d
		}
	}
	existing, err := devices.List(ctx)
	if err != nil {
		return err
	}
	n := 0
	for _, d := range existing {
		def, ok := byTopic[d.MqttTopic]
		if !ok {
			continue
		}
		if d.DeviceType == "air" && d.Name == def.Name {
			continue
		}
		icon := d.Icon
		if icon == "" {
			icon = layout.IconFor(def.Icon, def.Name, "air")
		}
		if _, err := devices.Update(ctx, d.ID, repository.WriteDevice{
			Name: def.Name, MqttTopic: d.MqttTopic, DeviceType: "air", Unit: d.Unit,
			PowerTopic: d.PowerTopic, RoomID: d.RoomID, Icon: icon,
			X: d.X, Y: d.Y, W: d.W, H: d.H, Active: d.Active,
			WbDevice: d.WbDevice, WbControl: d.WbControl, Source: d.Source,
		}); err != nil {
			return err
		}
		n++
	}
	if n > 0 {
		log.Info().Int("devices", n).Msg("seed backfill air")
	}
	return nil
}
