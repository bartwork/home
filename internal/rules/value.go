package rules

import (
	"strconv"
	"strings"
)

func (e *Engine) resolve(ref ValueRef, vars map[string]string) string {
	if t := strings.TrimSpace(ref.Topic); t != "" {
		return e.state.Get(t)
	}
	if v := strings.TrimSpace(ref.Var); v != "" {
		return vars[v]
	}
	return ref.Const
}

func compareValues(op, left, right string) bool {
	op = strings.ToLower(strings.TrimSpace(op))
	lf, lOk := parseFloat(left)
	rf, rOk := parseFloat(right)
	if lOk && rOk {
		switch op {
		case "eq":
			return lf == rf
		case "ne":
			return lf != rf
		case "lt":
			return lf < rf
		case "lte":
			return lf <= rf
		case "gt":
			return lf > rf
		case "gte":
			return lf >= rf
		}
	}
	switch op {
	case "eq":
		return left == right
	case "ne":
		return left != right
	case "lt":
		return left < right
	case "lte":
		return left <= right
	case "gt":
		return left > right
	case "gte":
		return left >= right
	default:
		return false
	}
}

func parseFloat(s string) (float64, bool) {
	s = strings.TrimSpace(strings.ReplaceAll(s, ",", "."))
	if s == "" {
		return 0, false
	}
	f, err := strconv.ParseFloat(s, 64)
	if err != nil {
		return 0, false
	}
	return f, true
}
