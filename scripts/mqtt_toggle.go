//go:build ignore

package main

import (
	"fmt"
	"os"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

func main() {
	broker := env("MQTT_BROKER", "tcp://192.168.1.9:1883")
	topic := env("TOPIC", "/devices/wb-mr6cv3_225/controls/K6/on")
	payload := env("PAYLOAD", "0")

	opts := mqtt.NewClientOptions().
		AddBroker(broker).
		SetClientID(fmt.Sprintf("home-toggle-%d", time.Now().UnixNano())).
		SetConnectTimeout(5 * time.Second)
	c := mqtt.NewClient(opts)
	tok := c.Connect()
	tok.Wait()
	if err := tok.Error(); err != nil {
		fmt.Fprintf(os.Stderr, "connect: %v\n", err)
		os.Exit(1)
	}
	defer c.Disconnect(250)

	pt := c.Publish(topic, 0, false, payload)
	pt.Wait()
	if err := pt.Error(); err != nil {
		fmt.Fprintf(os.Stderr, "publish: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("ok %s <- %q\n", topic, payload)
}

func env(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}
