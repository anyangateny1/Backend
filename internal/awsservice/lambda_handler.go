// / package awsgateway Contains all the configuration values for the AWS portal
package awsservice

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/smtp"
	"os"

	"github.com/aws/aws-lambda-go/events"
)

func HandleRequest(ctx context.Context, req events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	s3Client, err := newS3Client(
		context.Background(),
		"anyang-personal-website",
	)
	if err != nil {
		log.Fatal("Could not start S3 client, %w", err)
	}

	switch {
	case req.RequestContext.HTTP.Method == "GET" && req.RawPath == "/projects":
		return getProjects(ctx, req, s3Client)

	case req.RequestContext.HTTP.Method == "POST" && req.RawPath == "/contact":
		return postContact(ctx, req)

	default:
		return events.APIGatewayV2HTTPResponse{
			StatusCode: 404,
			Body:       "Not found",
		}, nil
	}

	// return events.APIGatewayV2HTTPResponse{}, nil
}

type Project struct {
	ID          int      `json:"id"`
	ProjectName string   `json:"projectName"`
	ProjectDate string   `json:"projectDate"`
	Desc        string   `json:"desc"`
	ImgURL      string   `json:"imgUrl"`
	Tags        []string `json:"tags"`
}

func getProjects(
	ctx context.Context,
	req events.APIGatewayV2HTTPRequest,
	c *S3Client,
) (events.APIGatewayV2HTTPResponse, error) {
	const projectKey = "files/projects.json"

	bodyBytes, err := c.readJSONFile(ctx, projectKey)
	if err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf(
			"failed to get projects: %w",
			err,
		)
	}

	var projects []Project

	if err := json.Unmarshal(bodyBytes, &projects); err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf(
			"failed to unmarshal projects: %w",
			err,
		)
	}

	for i := range projects {
		imageKey := "images/" + projects[i].ImgURL

		imageURL, err := c.getPresignedURL(ctx, imageKey)
		if err != nil {
			return events.APIGatewayV2HTTPResponse{}, fmt.Errorf(
				"failed to generate presigned URL: %w",
				err,
			)
		}

		projects[i].ImgURL = imageURL
	}

	responseBody, err := json.Marshal(projects)
	if err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf(
			"failed to marshal projects: %w",
			err,
		)
	}

	return events.APIGatewayV2HTTPResponse{
		StatusCode: http.StatusOK,
		Headers: map[string]string{
			"Content-Type": "application/json",
		},
		Body: string(responseBody),
	}, nil
}

type emailMessage struct {
	Name    string `json:"name"`
	Email   string `json:"email"`
	Message string `json:"subject"`
}

func postContact(
	ctx context.Context,
	req events.APIGatewayV2HTTPRequest,
) (events.APIGatewayV2HTTPResponse, error) {
	// TODO: Split this into a SMTP config

	host, ok := os.LookupEnv("SMTP_HOST")
	if !ok {
		log.Fatal("SMTP_HOST is not set")
	}

	username, ok := os.LookupEnv("SMTP_USER")
	if !ok {
		log.Fatal("SMTP_USER is not set")
	}

	password, ok := os.LookupEnv("SMTP_PASSWORD")
	if !ok {
		log.Fatal("SMTP_PASSWORD is not set")
	}
	auth := smtp.PlainAuth("", username, password, host)

	var emailMessage emailMessage
	if err := json.Unmarshal([]byte(req.Body), &emailMessage); err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf(
			"failed to unmarshal email: %w",
			err,
		)
	}

	// TODO: Add more INFO, DEBUG logging
	en := emailMessage.Name
	ee := emailMessage.Email
	em := emailMessage.Message

	msgStr := fmt.Sprintf(
		"Subject: atenyanyang.com\r\n"+
			"\r\n"+
			"New email from atenyanyang.com\r\n"+
			"Name: %s\r\n"+
			"Email: %s\r\n"+
			"Message: %s",
		en, ee, em,
	)

	msg := []byte(msgStr)

	port, ok := os.LookupEnv("SMTP_PORT")
	if !ok {
		log.Fatal("SMTP_PORT is not set")
	}

	to := []string{os.Getenv("EMAIL_TO")}

	from, ok := os.LookupEnv("EMAIL_FROM")
	if !ok {
		log.Fatal("EMAIL_FROM is not set")
	}

	addr := host + ":" + port
	err := smtp.SendMail(addr, auth, from, to, msg)
	if err != nil {
		log.Fatal("Send mail failed, %w", err)
	}

	return events.APIGatewayV2HTTPResponse{
		StatusCode: http.StatusOK,
	}, nil
}
