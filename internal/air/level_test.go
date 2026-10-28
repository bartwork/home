package air

import "testing"

func TestNextLevel(t *testing.T) {
	if got := NextLevel("", 700); got != LevelGood {
		t.Fatalf("700: %s", got)
	}
	if got := NextLevel("", 1000); got != LevelOK {
		t.Fatalf("1000: %s", got)
	}
	if got := NextLevel("", 1300); got != LevelHigh {
		t.Fatalf("1300: %s", got)
	}
	if got := NextLevel(LevelHigh, 1150); got != LevelHigh {
		t.Fatalf("high@1150: %s", got)
	}
	if got := NextLevel(LevelHigh, 1000); got != LevelOK {
		t.Fatalf("high→ok: %s", got)
	}
	if got := NextLevel(LevelGood, 850); got != LevelGood {
		t.Fatalf("good@850: %s", got)
	}
	if got := NextLevel(LevelGood, 950); got != LevelOK {
		t.Fatalf("good→ok: %s", got)
	}
}

func TestLEDPayloads(t *testing.T) {
	r, g := LEDPayloads(LevelHigh)
	if r != "1" || g != "0" {
		t.Fatalf("high: %s/%s", r, g)
	}
	r, g = LEDPayloads(LevelGood)
	if r != "0" || g != "1" {
		t.Fatalf("good: %s/%s", r, g)
	}
	r, g = LEDPayloads(LevelOK)
	if r != "0" || g != "0" {
		t.Fatalf("ok: %s/%s", r, g)
	}
}
