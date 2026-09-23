package auth_test

import (
	"testing"
	"time"

	"github.com/bartwork/home/internal/auth"
	"github.com/golang-jwt/jwt/v5"
)

func TestTokensRoundTrip(t *testing.T) {
	tokens := auth.NewTokens("secret")
	raw, err := tokens.Issue(42)
	if err != nil {
		t.Fatal(err)
	}
	claims, err := tokens.Parse(raw)
	if err != nil {
		t.Fatal(err)
	}
	if claims.UserID != 42 {
		t.Fatalf("uid=%d", claims.UserID)
	}
}

func TestTokensRejectWrongAlg(t *testing.T) {
	tokens := auth.NewTokens("secret")
	tok := jwt.NewWithClaims(jwt.SigningMethodNone, auth.Claims{
		UserID: 1,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Hour)),
		},
	})
	raw, err := tok.SignedString(jwt.UnsafeAllowNoneSignatureType)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := tokens.Parse(raw); err == nil {
		t.Fatal("expected error for none alg")
	}
}

func TestTokensRejectBadSecret(t *testing.T) {
	a := auth.NewTokens("a")
	b := auth.NewTokens("b")
	raw, err := a.Issue(1)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := b.Parse(raw); err == nil {
		t.Fatal("expected error for wrong secret")
	}
}
