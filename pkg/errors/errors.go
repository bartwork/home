package errors

import "net/http"

// Error is an API error with an HTTP status code (used by tg transport).
type Error struct {
	msg  string
	code int
}

func (e Error) Error() string { return e.msg }
func (e Error) Code() int     { return e.code }

var (
	ErrNotFound   = Error{msg: "not found", code: http.StatusNotFound}
	ErrBadRequest = Error{msg: "bad request", code: http.StatusBadRequest}
	ErrConflict   = Error{msg: "conflict", code: http.StatusConflict}
)
