package layout

import "testing"

func TestClampRect(t *testing.T) {
	x, y, w, h := ClampRect(-10, -5, 200, 1)
	if x != 0 || y != 0 || w > 100 || h < 5 {
		t.Fatalf("got %v %v %v %v", x, y, w, h)
	}
	x, y, w, h = ClampRect(90, 90, 20, 20)
	if x+w > 100.001 || y+h > 100.001 {
		t.Fatalf("overflow %v+%v %v+%v", x, w, y, h)
	}
}
