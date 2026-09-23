package contracts

import (
	"context"

	"github.com/bartwork/home/contracts/dto"
)

// @tg http-prefix=api/v1
// @tg http-server log metrics
// @tg swaggerTags=auth
type Auth interface {
	// @tg summary=`Вход`
	// @tg http-method=POST
	// @tg http-path=auth/login
	// @tg http-success=200
	Login(ctx context.Context, login string, password string) (token string, user dto.User, err error)
}
