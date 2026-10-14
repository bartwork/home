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
	msg := err.Error()
	if strings.Contains(msg, "UNIQUE constraint failed") {
		return apperr.ErrConflict
	}
	if strings.Contains(msg, "FOREIGN KEY constraint failed") {
		return apperr.ErrBadRequest
	}
	return err
}
