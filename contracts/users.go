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

	// @tg summary=`Пользователь`
	// @tg http-method=GET
	// @tg http-path=users/:id
	// @tg http-success=200
	GetUser(ctx context.Context, id int64) (user dto.User, err error)

	// @tg summary=`Создать пользователя`
	// @tg http-method=POST
	// @tg http-path=users
	// @tg http-success=201
	CreateUser(ctx context.Context, in dto.CreateUser) (user dto.User, err error)

	// @tg summary=`Обновить пользователя`
	// @tg http-method=PUT
	// @tg http-path=users/:id
	// @tg http-success=200
	UpdateUser(ctx context.Context, id int64, in dto.UpdateUser) (user dto.User, err error)

	// @tg summary=`Удалить пользователя`
	// @tg http-method=DELETE
	// @tg http-path=users/:id
	// @tg http-success=204
	DeleteUser(ctx context.Context, id int64) (err error)
}
