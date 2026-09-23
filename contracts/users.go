package contracts

import (
	"context"

	"github.com/bartwork/home/contracts/dto"
)

// @tg http-prefix=api/v1
// @tg http-server log metrics
// @tg swaggerTags=users
type Users interface {
	// @tg summary=`Список пользователей`
	// @tg http-method=GET
	// @tg http-path=users
	// @tg http-success=200
	ListUsers(ctx context.Context) (users []dto.User, err error)

	// @tg summary=`Пользователь и его устройства`
	// @tg http-method=GET
	// @tg http-path=users/:id
	// @tg http-success=200
	GetUser(ctx context.Context, id int64) (user dto.UserDetails, err error)
}
