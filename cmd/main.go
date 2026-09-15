package main

import (
	awsservice "github.com/anyangateny1/backend/m/v2/internal/awsservice"
	"github.com/aws/aws-lambda-go/lambda"
)

func main() {
	lambda.Start(awsservice.HandleRequest)
}
