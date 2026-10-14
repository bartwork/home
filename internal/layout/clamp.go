package layout

// ClampRect ограничивает позицию/размер в процентах плана (0–100).
func ClampRect(x, y, w, h float64) (float64, float64, float64, float64) {
	if w < 5 {
		w = 5
	}
	if h < 5 {
		h = 5
	}
	if w > 100 {
		w = 100
	}
	if h > 100 {
		h = 100
	}
	if x < 0 {
		x = 0
	}
	if y < 0 {
		y = 0
	}
	if x > 95 {
		x = 95
	}
	if y > 95 {
		y = 95
	}
	if x+w > 100 {
		w = 100 - x
	}
	if y+h > 100 {
		h = 100 - y
	}
	if w < 5 {
		w = 5
	}
	if h < 5 {
		h = 5
	}
	return x, y, w, h
}
