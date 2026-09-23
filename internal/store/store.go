package store

import (
	"context"
	"database/sql"
	"fmt"
	"net/url"
	"strings"

	"github.com/bartwork/home/db/migrations"
	_ "github.com/mutecomm/go-sqlcipher/v4"
	"github.com/pressly/goose/v3"
)

// SQLCipherKey encrypts the DB file; override with -ldflags "-X ...SQLCipherKey=...".
var SQLCipherKey = "home-dev-sqlcipher-key"

func Open(path string) (*sql.DB, error) {
	if strings.TrimSpace(SQLCipherKey) == "" {
		return nil, fmt.Errorf("empty SQLCipher key")
	}

	dsn := path + "?_pragma_key=" + url.QueryEscape(SQLCipherKey) + "&_pragma_cipher_page_size=4096"
	db, err := sql.Open("sqlite3", dsn)
	if err != nil {
		return nil, fmt.Errorf("open: %w", err)
	}
	db.SetMaxOpenConns(1)

	if err := db.Ping(); err != nil {
		_ = db.Close()
		return nil, fmt.Errorf("ping: %w", err)
	}

	for _, p := range []string{
		`PRAGMA foreign_keys = ON`,
		`PRAGMA busy_timeout = 5000`,
		`PRAGMA journal_mode = WAL`,
	} {
		if _, err := db.Exec(p); err != nil {
			_ = db.Close()
			return nil, fmt.Errorf("%s: %w", p, err)
		}
	}

	if err := migrate(db); err != nil {
		_ = db.Close()
		return nil, err
	}
	return db, nil
}

func migrate(db *sql.DB) error {
	provider, err := goose.NewProvider(goose.DialectSQLite3, db, migrations.FS)
	if err != nil {
		return fmt.Errorf("goose: %w", err)
	}
	if _, err := provider.Up(context.Background()); err != nil {
		return fmt.Errorf("migrate: %w", err)
	}
	return nil
}
