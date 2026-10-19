package dto

type ColorFavorite struct {
	ID         int64  `json:"id"`
	Hue        int64  `json:"hue"`
	Saturation int64  `json:"saturation"`
	Brightness int64  `json:"brightness"`
	SortOrder  int64  `json:"sortOrder"`
	CreatedAt  string `json:"createdAt"`
}
