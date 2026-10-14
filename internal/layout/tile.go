package layout

// Tile раскладывает квадратные ячейки внутри комнаты (%).
// UI рисует сетку; координаты сохраняются для порядка (y, x).
func Tile(index, cols int) (x, y, w, h float64) {
	if cols < 1 {
		cols = 3
	}
	const (
		gap  = 3.0
		size = 28.0
		ox   = 3.0
		oy   = 4.0
	)
	col := index % cols
	row := index / cols
	return ox + float64(col)*(size+gap), oy + float64(row)*(size+gap), size, size
}
