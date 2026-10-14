package repository

import "github.com/bartwork/home/contracts/dto"

func (d Device) ToDTO() dto.Device {
	return dto.Device{
		ID:         d.ID,
		Name:       d.Name,
		MqttTopic:  d.MqttTopic,
		DeviceType: d.DeviceType,
		Unit:       d.Unit,
		PowerTopic: d.PowerTopic,
		RoomID:     d.RoomID,
		RoomName:   d.RoomName,
		Icon:       d.Icon,
		X:          d.X,
		Y:          d.Y,
		W:          d.W,
		H:          d.H,
		Active:     d.Active,
		CreatedAt:  d.CreatedAt,
		WbDevice:   d.WbDevice,
		WbControl:  d.WbControl,
		Source:     d.Source,
	}
}
