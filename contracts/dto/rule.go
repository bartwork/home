package dto

import "encoding/json"

type Rule struct {
	ID         int64           `json:"id"`
	Name       string          `json:"name"`
	Enabled    bool            `json:"enabled"`
	Mode       string          `json:"mode"`
	CooldownMs int64           `json:"cooldownMs"`
	Graph      json.RawMessage `json:"graph"`
	CreatedAt  string          `json:"createdAt"`
	UpdatedAt  string          `json:"updatedAt"`
}
