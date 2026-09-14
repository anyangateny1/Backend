// / package awsgateway Contains all the configuration values for the AWS portal
package awsservice

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/aws/aws-lambda-go/events"
)

// type config struct {
// 	region      string
// 	bucket_name string
// }

// type AwsGateway struct {
// 	cfg config
// 	s3  s3.Client
// }

type RequestBody struct {
	Name string `json:"name"`
}

type ResponseBody struct {
	Message string `json:"message"`
}

func HandleRequest(ctx context.Context, req events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	s3Client, err := newS3Client(
		context.Background(),
		"anyang-personal-website",
	)
	if err != nil {
		log.Fatalf("Could not start S3 client, %w", err)
	}

	switch {
	case req.RequestContext.HTTP.Method == "GET" && req.RawPath == "/projects":
		return getProjects(ctx, req, s3Client)

	// case req.RequestContext.HTTP.Method == "GET" && req.RawPath == "/image":
	// 	return getImage(ctx, req, s3Client)
	//
	// case req.RequestContext.HTTP.Method == "GET" && req.RawPath == "/resume":
	// 	return getResume(ctx, req)
	//
	// case req.RequestContext.HTTP.Method == "POST" && req.RawPath == "/contact":
	// 	return postContact(ctx, req)

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

// func getImage(
// 	ctx context.Context,
// 	req events.APIGatewayV2HTTPRequest,
// 	c *S3Client,
// ) (events.APIGatewayV2HTTPResponse, error) {
// 	signedImageUrl := req.RawPath
// }
