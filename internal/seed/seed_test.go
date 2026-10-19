package seed

import (
	"strings"
	"testing"

	"github.com/bartwork/home/internal/layout"
)

func TestTile(t *testing.T) {
	x0, y0, w, h := layout.Tile(0, 3)
	if w != 28 || h != 28 {
		t.Fatalf("size: %v %v", w, h)
	}
	x1, y1, _, _ := layout.Tile(1, 3)
	if x1 <= x0 || y1 != y0 {
		t.Fatalf("col layout: (%v,%v) (%v,%v)", x0, y0, x1, y1)
	}
	_, y3, _, _ := layout.Tile(3, 3)
	if y3 <= y0 {
		t.Fatalf("row layout: y0=%v y3=%v", y0, y3)
	}
}

func TestIconFor(t *testing.T) {
	cases := []struct {
		icon, name, typ, want string
	}{
		{"fan", "x", "switch", "fan"},
		{"", "вытяжка", "switch", "fan"},
		{"", "тёплый пол", "floor", "thermo"},
		{"", "x", "floor", "thermo"},
		{"", "свет", "switch", "light"},
		{"", "люстра", "range", "dimmer"},
		{"", "темп", "sensor", "sensor"},
	}
	for _, c := range cases {
		if got := layout.IconFor(c.icon, c.name, c.typ); got != c.want {
			t.Fatalf("%q/%q/%q: got %q want %q", c.icon, c.name, c.typ, got, c.want)
		}
	}
}

func TestCatalogTopicsUnique(t *testing.T) {
	seen := map[string]string{}
	for _, d := range Devices {
		if d.Topic == "" || strings.Contains(d.Topic, "?") {
			continue
		}
		if prev, ok := seen[d.Topic]; ok {
			t.Fatalf("duplicate topic %q: %q and %q", d.Topic, prev, d.Name)
		}
		seen[d.Topic] = d.Name
	}
}
