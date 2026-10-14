package discovery_test

import (
	"context"
	"path/filepath"
	"testing"

	"github.com/bartwork/home/internal/discovery"
	"github.com/bartwork/home/internal/repository"
	"github.com/bartwork/home/internal/store"
	"github.com/rs/zerolog"
)

func TestEngineObserveAndAccept(t *testing.T) {
	db, err := store.Open(filepath.Join(t.TempDir(), "home.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = db.Close() })

	disco := repository.NewDiscovered(db)
	devs := repository.NewDevices(db)
	rooms := repository.NewRooms(db)
	engine := discovery.NewEngine(zerolog.Nop(), disco, devs)
	svc := discovery.NewService(disco, devs, rooms)
	ctx := context.Background()

	engine.Observe("/devices/wb-gpio/controls/K1/meta/type", "switch")
	engine.Observe("/devices/wb-gpio/controls/K1/meta/title", "Реле K1")
	engine.Observe("/devices/wb-gpio/controls/K1", "0")

	list, err := svc.ListNew(ctx)
	if err != nil {
		t.Fatal(err)
	}
	if len(list) != 1 {
		t.Fatalf("want 1 discovered, got %d", len(list))
	}
	if list[0].SuggestedType != "switch" || list[0].Title != "Реле K1" {
		t.Fatalf("%+v", list[0])
	}

	dev, err := svc.Accept(ctx, list[0].ID, discovery.AcceptOpts{Name: "тест реле"})
	if err != nil {
		t.Fatal(err)
	}
	if dev.DeviceType != "switch" || dev.WbDevice != "wb-gpio" || dev.Source != "discovery" {
		t.Fatalf("%+v", dev)
	}

	n, err := svc.CountNew(ctx)
	if err != nil || n != 0 {
		t.Fatalf("count=%d err=%v", n, err)
	}

	// повторный observe не создаёт new
	engine.Observe("/devices/wb-gpio/controls/K1", "1")
	n, _ = svc.CountNew(ctx)
	if n != 0 {
		t.Fatalf("accepted topic reappeared: %d", n)
	}

	// удалили устройство — следующий observe снова new
	if err := devs.Delete(ctx, dev.ID); err != nil {
		t.Fatal(err)
	}
	engine.Observe("/devices/wb-gpio/controls/K1", "0")
	n, err = svc.CountNew(ctx)
	if err != nil || n != 1 {
		t.Fatalf("want rediscovery count=1, got %d err=%v", n, err)
	}
}
