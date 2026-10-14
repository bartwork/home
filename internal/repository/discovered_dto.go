package repository

import "github.com/bartwork/home/contracts/dto"

func (d Discovered) ToDTO() dto.Discovered {
	return dto.Discovered{
		ID:            d.ID,
		MqttTopic:     d.MqttTopic,
		WbDevice:      d.WbDevice,
		WbControl:     d.WbControl,
		Title:         d.Title,
		SuggestedType: d.SuggestedType,
		Unit:          d.Unit,
		LastPayload:   d.LastPayload,
		LastSeenAt:    d.LastSeenAt,
		Status:        d.Status,
	}
}
