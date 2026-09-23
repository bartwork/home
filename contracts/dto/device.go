package dto

type Device struct {
	ID         int64  `json:"id"`
	Name       string `json:"name"`
	MqttTopic  string `json:"mqttTopic"`
	DeviceType string `json:"deviceType"`
	Unit       string `json:"unit,omitempty"`
	Active     bool   `json:"active"`
	CreatedAt  string `json:"createdAt,omitempty"`
}

type CreateDevice struct {
	Name       string `json:"name"`
	MqttTopic  string `json:"mqttTopic"`
	DeviceType string `json:"deviceType"`
	Unit       string `json:"unit"`
	Active     *bool  `json:"active,omitempty"`
}

type UpdateDevice struct {
	Name       string `json:"name"`
	MqttTopic  string `json:"mqttTopic"`
	DeviceType string `json:"deviceType"`
	Unit       string `json:"unit"`
	Active     bool   `json:"active"`
}
