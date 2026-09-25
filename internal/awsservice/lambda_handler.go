// / package awsgateway Contains all the configuration values for the AWS portal
package awsservice

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/smtp"
	"strings"

	"github.com/aws/aws-lambda-go/events"

	smtpconfig "github.com/anyangateny1/backend/m/v2/config/smtpconfig"
)

const (
	s3Bucket    = "anyang-personal-website"
	projectsKey = "files/projects.json"
	siteName    = "atenyanyang.com"
)

func HandleRequest(ctx context.Context, req events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	s3Client, err := newS3Client(ctx, s3Bucket)
	if err != nil {
		log.Printf("failed to start S3 client: %v", err)
		return internalError(), nil
	}

	method := req.RequestContext.HTTP.Method

	switch {
	case method == http.MethodGet && req.RawPath == "/projects":
		return getProjects(ctx, s3Client)

	case method == http.MethodPost && req.RawPath == "/contact":
		return postContact(ctx, req)

	default:
		return events.APIGatewayV2HTTPResponse{
			StatusCode: http.StatusNotFound,
			Body:       "Not found",
		}, nil
	}
}

func internalError() events.APIGatewayV2HTTPResponse {
	return events.APIGatewayV2HTTPResponse{
		StatusCode: http.StatusInternalServerError,
		Body:       "Internal server error",
	}
}

type Project struct {
	ID          int      `json:"id"`
	ProjectName string   `json:"name"`
	ProjectDate string   `json:"date"`
	Desc        string   `json:"description"`
	ImgURL      string   `json:"imgUrl"`
	GithubUrl   string   `json:"githubUrl"`
	Tags        []string `json:"tags"`
}

func getProjects(ctx context.Context, c *S3Client) (events.APIGatewayV2HTTPResponse, error) {
	bodyBytes, err := c.readJSONFile(ctx, projectsKey)
	if err != nil {
		log.Printf("failed to read projects file: %v", err)
		return internalError(), nil
	}

	var projects []Project
	if err := json.Unmarshal(bodyBytes, &projects); err != nil {
		log.Printf("failed to unmarshal projects: %v", err)
		return internalError(), nil
	}

	for i := range projects {
		imageKey := "images/" + projects[i].ImgURL

		imageURL, err := c.getPresignedURL(ctx, imageKey)
		if err != nil {
			log.Printf("failed to presign %q: %v", imageKey, err)
			return internalError(), nil
		}

		projects[i].ImgURL = imageURL
	}

	responseBody, err := json.Marshal(projects)
	if err != nil {
		log.Printf("failed to marshal projects: %v", err)
		return internalError(), nil
	}

	return events.APIGatewayV2HTTPResponse{
		StatusCode: http.StatusOK,
		Headers: map[string]string{
			"Content-Type": "application/json",
		},
		Body: string(responseBody),
	}, nil
}

type contactRequest struct {
	Name    string `json:"name"`
	Email   string `json:"email"`
	Message string `json:"subject"`
}

func postContact(ctx context.Context, req events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	cfg, err := smtpconfig.LoadSMTPConfig()
	if err != nil {
		log.Printf("smtp config error: %v", err)
		return internalError(), nil
	}

	var contact contactRequest
	if err := json.Unmarshal([]byte(req.Body), &contact); err != nil {
		log.Printf("failed to unmarshal contact request: %v", err)
		return events.APIGatewayV2HTTPResponse{
			StatusCode: http.StatusBadRequest,
			Body:       "Invalid request body",
		}, nil
	}

	if strings.TrimSpace(contact.Name) == "" || strings.TrimSpace(contact.Email) == "" {
		return events.APIGatewayV2HTTPResponse{
			StatusCode: http.StatusBadRequest,
			Body:       "name and email are required",
		}, nil
	}

	msg := buildEmailMessage(contact)

	auth := smtp.PlainAuth("", cfg.Username, cfg.Password, cfg.Host)
	addr := cfg.Host + ":" + cfg.Port

	if err := smtp.SendMail(addr, auth, cfg.From, []string{cfg.To}, msg); err != nil {
		log.Printf("failed to send mail: %v", err)
		return internalError(), nil
	}

	return events.APIGatewayV2HTTPResponse{
		StatusCode: http.StatusOK,
	}, nil
}

func buildEmailMessage(c contactRequest) []byte {
	sanitize := func(s string) string {
		s = strings.ReplaceAll(s, "\r", "")
		s = strings.ReplaceAll(s, "\n", "")
		return s
	}

	name := sanitize(c.Name)
	email := sanitize(c.Email)
	message := sanitize(c.Message)

	body := fmt.Sprintf(
		"Subject: %s\r\n\r\n"+
			"New email from %s\r\n"+
			"Name: %s\r\n"+
			"Email: %s\r\n"+
			"Message: %s",
		siteName, siteName, name, email, message,
	)

	return []byte(body)
}
