package dto

type User struct {
	ID    int64  `json:"id"`
	Login string `json:"login"`
	Name  string `json:"name"`
}

type Device struct {
	ID          int64  `json:"id"`
	Name        string `json:"name"`
	Fingerprint string `json:"fingerprint"`
	LastSeen    string `json:"lastSeen"`
}

type UserDetails struct {
	User    User     `json:"user"`
	Devices []Device `json:"devices"`
}
