package colors

// defaultPalette — стандартный набор для ленты (HSV), если избранное пусто.
// Тёплый → холодный → акценты, все на полной яркости.
var defaultPalette = []CreateIn{
	{Hue: 28, Saturation: 85, Brightness: 100},  // янтарь
	{Hue: 48, Saturation: 70, Brightness: 100},  // тёплый жёлтый
	{Hue: 0, Saturation: 95, Brightness: 100},   // красный
	{Hue: 330, Saturation: 75, Brightness: 100}, // розовый
	{Hue: 280, Saturation: 80, Brightness: 100}, // фиолетовый
	{Hue: 210, Saturation: 85, Brightness: 100}, // синий
	{Hue: 175, Saturation: 70, Brightness: 100}, // бирюза
	{Hue: 130, Saturation: 70, Brightness: 100}, // зелёный
}
