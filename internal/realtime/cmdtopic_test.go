package realtime_test

import (
	"testing"

	"github.com/bartwork/home/internal/realtime"
)

func TestCommandTopic(t *testing.T) {
	got := realtime.CommandTopic("/devices/wb-mdm3_76/controls/Channel 1")
	want := "/devices/wb-mdm3_76/controls/Channel 1/on"
	if got != want {
		t.Fatalf("got %q want %q", got, want)
	}
}
