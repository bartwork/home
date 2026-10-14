package dto

type Device struct {
	ID         int64   `json:"id"`
	Name       string  `json:"name"`
	MqttTopic  string  `json:"mqttTopic"`
	DeviceType string  `json:"deviceType"`
	Unit       string  `json:"unit"`
	PowerTopic string  `json:"powerTopic"`
	RoomID     *int64  `json:"roomId,omitempty"`
	RoomName   string  `json:"roomName"`
	Icon       string  `json:"icon"`
	X          float64 `json:"x"`
	Y          float64 `json:"y"`
	W          float64 `json:"w"`
	H          float64 `json:"h"`
	Active     bool    `json:"active"`
	CreatedAt  string  `json:"createdAt"`
	WbDevice   string  `json:"wbDevice"`
	WbControl  string  `json:"wbControl"`
	Source     string  `json:"source"`
}

type Discovered struct {
	ID            int64  `json:"id"`
	MqttTopic     string `json:"mqttTopic"`
	WbDevice      string `json:"wbDevice"`
	WbControl     string `json:"wbControl"`
	Title         string `json:"title"`
	SuggestedType string `json:"suggestedType"`
	Unit          string `json:"unit"`
	LastPayload   string `json:"lastPayload"`
	LastSeenAt    string `json:"lastSeenAt"`
	Status        string `json:"status"`
}

type Room struct {
	ID        int64   `json:"id"`
	Name      string  `json:"name"`
	X         float64 `json:"x"`
	Y         float64 `json:"y"`
	W         float64 `json:"w"`
	H         float64 `json:"h"`
	SortOrder int64   `json:"sortOrder"`
}
