package contracts

import (
	"context"

	"github.com/bartwork/home/contracts/dto"
)

// @tg http-prefix=api/v1
// @tg http-server log metrics
// @tg swaggerTags=devices
type Devices interface {
	// @tg summary=`Список устройств`
	// @tg http-method=GET
	// @tg http-path=devices
	// @tg http-success=200
	ListDevices(ctx context.Context) (devices []dto.Device, err error)

	// @tg summary=`Устройство`
	// @tg http-method=GET
	// @tg http-path=devices/:id
	// @tg http-success=200
	GetDevice(ctx context.Context, id int64) (device dto.Device, err error)

	// @tg summary=`Создать устройство`
	// @tg http-method=POST
	// @tg http-path=devices
	// @tg http-success=201
	CreateDevice(ctx context.Context, name string, mqttTopic string, deviceType string, unit string, active bool) (device dto.Device, err error)

	// @tg summary=`Обновить устройство`
	// @tg http-method=PUT
	// @tg http-path=devices/:id
	// @tg http-success=200
	UpdateDevice(ctx context.Context, id int64, name string, mqttTopic string, deviceType string, unit string, active bool) (device dto.Device, err error)

	// @tg summary=`Удалить устройство`
	// @tg http-method=DELETE
	// @tg http-path=devices/:id
	// @tg http-success=204
	DeleteDevice(ctx context.Context, id int64) (err error)
}
