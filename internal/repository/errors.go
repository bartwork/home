package repository

import (
	"database/sql"
	"errors"
	"strings"

	apperr "github.com/bartwork/home/pkg/errors"
)

func mapDBErr(err error) error {
	if err == nil {
		return nil
	}
	if errors.Is(err, sql.ErrNoRows) {
		return apperr.ErrNotFound
	}
	if strings.Contains(err.Error(), "UNIQUE constraint failed") {
		return apperr.ErrConflict
	}
	return err
}
