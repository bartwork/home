package dto

type Device struct {
	ID         int64  `json:"id"`
	Name       string `json:"name"`
	MqttTopic  string `json:"mqttTopic"`
	DeviceType string `json:"deviceType"`
	Unit       string `json:"unit,omitempty"`
	Active     bool   `json:"active"`
	CreatedAt  string `json:"createdAt,omitempty"`
	WbDevice   string `json:"wbDevice,omitempty"`
	WbControl  string `json:"wbControl,omitempty"`
	Source     string `json:"source,omitempty"`
}

type Discovered struct {
	ID            int64  `json:"id"`
	MqttTopic     string `json:"mqttTopic"`
	WbDevice      string `json:"wbDevice"`
	WbControl     string `json:"wbControl"`
	Title         string `json:"title"`
	SuggestedType string `json:"suggestedType"`
	Unit          string `json:"unit,omitempty"`
	LastPayload   string `json:"lastPayload,omitempty"`
	LastSeenAt    string `json:"lastSeenAt,omitempty"`
	Status        string `json:"status"`
}
