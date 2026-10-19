package rules

import apperr "github.com/bartwork/home/pkg/errors"

var (
	errRuleNotLoaded   = apperr.ErrNotFound
	errRuleDisabled    = apperr.ErrBadRequest
	errNoManualTrigger = apperr.ErrBadRequest
)
