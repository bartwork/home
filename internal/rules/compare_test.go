package rules

import "testing"

func TestCompareValues(t *testing.T) {
	tests := []struct {
		op, left, right string
		want            bool
	}{
		{"eq", "1", "1", true},
		{"ne", "1", "0", true},
		{"lt", "21.5", "26", true},
		{"lte", "26", "26", true},
		{"gt", "27", "26", true},
		{"gte", "26", "26", true},
		{"lt", "27", "26", false},
		{"eq", "on", "on", true},
		{"eq", "on", "off", false},
	}
	for _, tt := range tests {
		if got := compareValues(tt.op, tt.left, tt.right); got != tt.want {
			t.Fatalf("%s %q %q: got %v want %v", tt.op, tt.left, tt.right, got, tt.want)
		}
	}
}
