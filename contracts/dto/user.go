package dto

type User struct {
	ID         int64  `json:"id"`
	LastName   string `json:"lastName"`
	FirstName  string `json:"firstName"`
	SecondName string `json:"secondName"`
	Email      string `json:"email"`
	Phone      string `json:"phone"`
	LastAuthAt string `json:"lastAuthAt,omitempty"`
	Active     bool   `json:"active"`
}

type CreateUser struct {
	LastName   string `json:"lastName"`
	FirstName  string `json:"firstName"`
	SecondName string `json:"secondName"`
	Email      string `json:"email"`
	Phone      string `json:"phone"`
	Password   string `json:"password"`
	Active     *bool  `json:"active,omitempty"`
}

type UpdateUser struct {
	LastName   string  `json:"lastName"`
	FirstName  string  `json:"firstName"`
	SecondName string  `json:"secondName"`
	Email      string  `json:"email"`
	Phone      string  `json:"phone"`
	Password   *string `json:"password,omitempty"`
	Active     bool    `json:"active"`
}
