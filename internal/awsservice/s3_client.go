package awsservice

import (
	"context"
	"io"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

type S3Client struct {
	bucketName string
	client     *s3.Client
	presigner  *s3.PresignClient
}

func newS3Client(ctx context.Context, bucketName string) (*S3Client, error) {
	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		return nil, err
	}

	client := s3.NewFromConfig(cfg)

	return &S3Client{
		bucketName: bucketName,
		client:     client,
		presigner:  s3.NewPresignClient(client),
	}, nil
}

func (c *S3Client) readJSONFile(ctx context.Context, key string) ([]byte, error) {
	output, err := c.client.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String(c.bucketName),
		Key:    aws.String(key),
	})
	if err != nil {
		return nil, err
	}
	defer output.Body.Close()

	return io.ReadAll(output.Body)
}

func (c *S3Client) getPresignedURL(ctx context.Context, key string) (string, error) {
	request, err := c.presigner.PresignGetObject(
		ctx,
		&s3.GetObjectInput{
			Bucket: aws.String(c.bucketName),
			Key:    aws.String(key),
		},
		func(opts *s3.PresignOptions) {
			opts.Expires = time.Minute * 60
		},
	)
	if err != nil {
		return "", err
	}

	return request.URL, nil
}
