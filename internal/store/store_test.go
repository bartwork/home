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
		"id", "last_name", "first_name", "second_name",
		"email", "phone", "password_hash", "last_auth_at", "is_active", "created_at",
	} {
		if !cols[want] {
			t.Fatalf("missing column %q", want)
		}
	}
}

func TestDatabaseFileIsEncrypted(t *testing.T) {
	path := filepath.Join(t.TempDir(), "home.db")
	db, err := store.Open(path)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := db.Exec(`INSERT INTO users (email, phone) VALUES ('a@b.c', '+1')`); err != nil {
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
