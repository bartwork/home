package store_test

import (
	"bytes"
	"os"
	"path/filepath"
	"testing"

	"github.com/bartwork/home/internal/store"
)

func TestOpenMigratesUsers(t *testing.T) {
	path := filepath.Join(t.TempDir(), "home.db")
	db, err := store.Open(path)
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()

	var n int
	if err := db.QueryRow(`SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='users'`).Scan(&n); err != nil {
		t.Fatal(err)
	}
	if n != 1 {
		t.Fatal("users table missing")
	}

	cols := map[string]bool{}
	rows, err := db.Query(`PRAGMA table_info(users)`)
	if err != nil {
		t.Fatal(err)
	}
	defer rows.Close()
	for rows.Next() {
		var cid int
		var name, typ string
		var notnull, pk int
		var dflt any
		if err := rows.Scan(&cid, &name, &typ, &notnull, &dflt, &pk); err != nil {
			t.Fatal(err)
		}
		cols[name] = true
	}

	for _, want := range []string{
		"id", "login", "last_name", "first_name", "second_name",
		"email", "phone", "password_hash", "last_auth_at", "is_active", "created_at",
	} {
		if !cols[want] {
			t.Fatalf("missing column %q", want)
		}
	}

	cols = map[string]bool{}
	drows, err := db.Query(`PRAGMA table_info(discovered_devices)`)
	if err != nil {
		t.Fatal(err)
	}
	defer drows.Close()
	for drows.Next() {
		var cid int
		var name, typ string
		var notnull, pk int
		var dflt any
		if err := drows.Scan(&cid, &name, &typ, &notnull, &dflt, &pk); err != nil {
			t.Fatal(err)
		}
		cols[name] = true
	}
	if !cols["mqtt_topic"] || !cols["status"] {
		t.Fatal("discovered_devices incomplete")
	}

	cols = map[string]bool{}
	devRows, err := db.Query(`PRAGMA table_info(devices)`)
	if err != nil {
		t.Fatal(err)
	}
	defer devRows.Close()
	for devRows.Next() {
		var cid int
		var name, typ string
		var notnull, pk int
		var dflt any
		if err := devRows.Scan(&cid, &name, &typ, &notnull, &dflt, &pk); err != nil {
			t.Fatal(err)
		}
		cols[name] = true
	}
	for _, want := range []string{"mqtt_topic", "device_type", "power_topic", "room_id", "icon", "x", "y", "w", "h", "wb_device"} {
		if !cols[want] {
			t.Fatalf("devices missing column %q", want)
		}
	}
	var nRooms int
	if err := db.QueryRow(`SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='rooms'`).Scan(&nRooms); err != nil {
		t.Fatal(err)
	}
	if nRooms != 1 {
		t.Fatal("rooms table missing")
	}
}

func TestDatabaseFileIsEncrypted(t *testing.T) {
	path := filepath.Join(t.TempDir(), "home.db")
	db, err := store.Open(path)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`INSERT INTO users (login, email, phone) VALUES ('alice', 'a@b.c', '+1')`); err != nil {
		t.Fatal(err)
	}
	if err := db.Close(); err != nil {
		t.Fatal(err)
	}

	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	if bytes.HasPrefix(raw, []byte("SQLite format 3")) {
		t.Fatal("database file looks unencrypted")
	}
	if bytes.Contains(raw, []byte("a@b.c")) || bytes.Contains(raw, []byte("users")) {
		t.Fatal("plaintext leaked into database file")
	}
}
