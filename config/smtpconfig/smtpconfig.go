package smtpconfig

import (
	"fmt"
	"os"
	"strings"
)

type smtpConfig struct {
	Host     string
	Port     string
	Username string
	Password string
	From     string
	To       string
}

func LoadSMTPConfig() (smtpConfig, error) {
	var cfg smtpConfig
	var missing []string

	get := func(key string) string {
		v, ok := os.LookupEnv(key)
		if !ok {
			missing = append(missing, key)
		}
		return v
	}

	cfg.Host = get("SMTP_HOST")
	cfg.Port = get("SMTP_PORT")
	cfg.Username = get("SMTP_USER")
	cfg.Password = get("SMTP_PASSWORD")
	cfg.From = get("EMAIL_FROM")
	cfg.To = get("EMAIL_TO")

	if len(missing) > 0 {
		return smtpConfig{}, fmt.Errorf("missing required env vars: %s", strings.Join(missing, ", "))
	}
	return cfg, nil
}
