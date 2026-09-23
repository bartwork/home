package users

import (
	"context"
	"sync"
	"time"

	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/pkg/errors"
)

type Service struct {
	mu    sync.RWMutex
	users map[int64]dto.User
	devs  map[int64][]dto.Device
}

func New() *Service {
	now := time.Now().UTC().Format(time.RFC3339)
	svc := &Service{
		users: map[int64]dto.User{
			1: {ID: 1, Login: "alice", Name: "Alice"},
			2: {ID: 2, Login: "bob", Name: "Bob"},
		},
		devs: map[int64][]dto.Device{
			1: {
				{ID: 1, Name: "iPhone 15", Fingerprint: "fp-alice-iphone", LastSeen: now},
				{ID: 2, Name: "MacBook Pro", Fingerprint: "fp-alice-mbp", LastSeen: now},
			},
			2: {
				{ID: 3, Name: "Pixel 8", Fingerprint: "fp-bob-pixel", LastSeen: now},
			},
		},
	}
	return svc
}

func (svc *Service) ListUsers(ctx context.Context) (users []dto.User, err error) {
	svc.mu.RLock()
	defer svc.mu.RUnlock()

	users = make([]dto.User, 0, len(svc.users))
	for _, u := range svc.users {
		users = append(users, u)
	}
	return users, nil
}

func (svc *Service) GetUser(ctx context.Context, id int64) (user dto.UserDetails, err error) {
	svc.mu.RLock()
	defer svc.mu.RUnlock()

	u, ok := svc.users[id]
	if !ok {
		err = errors.ErrNotFound
		return
	}

	devs := svc.devs[id]
	if devs == nil {
		devs = []dto.Device{}
	}

	user = dto.UserDetails{
		User:    u,
		Devices: append([]dto.Device(nil), devs...),
	}
	return user, nil
}
