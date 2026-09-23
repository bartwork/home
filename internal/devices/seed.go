package devices

import (
	"context"

	"github.com/bartwork/home/internal/repository"
)

func SeedDemo(ctx context.Context, repo repository.DeviceStore) error {
	list, err := repo.List(ctx)
	if err != nil {
		return err
	}
	if len(list) > 0 {
		return nil
	}

	demos := []repository.WriteDevice{
		{Name: "Реле K1", MqttTopic: "/devices/wb-gpio/controls/K1", DeviceType: "switch", Active: true},
		{Name: "Реле K2", MqttTopic: "/devices/wb-gpio/controls/K2", DeviceType: "switch", Active: true},
		{Name: "Температура", MqttTopic: "/devices/wb-msw-v4/controls/Temperature", DeviceType: "sensor", Unit: "°C", Active: true},
		{Name: "Влажность", MqttTopic: "/devices/wb-msw-v4/controls/Humidity", DeviceType: "sensor", Unit: "%", Active: true},
	}
	for _, d := range demos {
		if _, err := repo.Create(ctx, d); err != nil {
			return err
		}
	}
	return nil
}
