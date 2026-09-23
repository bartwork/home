package usermap

import (
	"github.com/bartwork/home/contracts/dto"
	"github.com/bartwork/home/internal/repository"
)

func DTO(u repository.User) dto.User {
	out := dto.User{
		ID:         u.ID,
		Login:      u.Login,
		LastName:   u.LastName,
		FirstName:  u.FirstName,
		SecondName: u.SecondName,
		Email:      u.Email,
		Phone:      u.Phone,
		Active:     u.Active,
	}
	if u.LastAuthAt != nil {
		out.LastAuthAt = *u.LastAuthAt
	}
	return out
}
