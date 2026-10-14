package repository

import "github.com/bartwork/home/contracts/dto"

func (u User) ToDTO() dto.User {
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
