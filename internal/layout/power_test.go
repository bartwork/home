package layout

import "testing"

func TestInferPowerTopic(t *testing.T) {
	cases := []struct {
		in, want string
	}{
		{"/devices/wb-mdm3_76/controls/Channel 3", "/devices/wb-mdm3_76/controls/K3"},
		{"/devices/wb-mdm3_75/controls/Channel 1", "/devices/wb-mdm3_75/controls/K1"},
		{"/devices/wb-led_221/controls/RGB Strip Brightness", ""},
		{"/devices/wb-led_221/controls/RGB Strip", ""},
		{"/devices/wb-mr6cv3_227/controls/K1", ""},
	}
	for _, c := range cases {
		if got := InferPowerTopic(c.in); got != c.want {
			t.Fatalf("%q: got %q want %q", c.in, got, c.want)
		}
	}
}
