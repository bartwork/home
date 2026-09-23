package errors

import (
	"encoding/json"
	"net/http"
)

// Error is an API error with an HTTP status code (used by tg transport).
type Error struct {
	msg  string
	code int
}

func (e Error) Error() string { return e.msg }
func (e Error) Code() int     { return e.code }

func (e Error) MarshalJSON() ([]byte, error) {
	return json.Marshal(struct {
		Msg  string `json:"msg"`
		Code int    `json:"code"`
	}{Msg: e.msg, Code: e.code})
}

var (
	ErrNotFound     = Error{msg: "not found", code: http.StatusNotFound}
	ErrBadRequest   = Error{msg: "bad request", code: http.StatusBadRequest}
	ErrConflict     = Error{msg: "conflict", code: http.StatusConflict}
	ErrUnauthorized = Error{msg: "unauthorized", code: http.StatusUnauthorized}
	ErrForbidden    = Error{msg: "forbidden", code: http.StatusForbidden}
)
