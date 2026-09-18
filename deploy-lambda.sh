#!/bin/bash

set -euo pipefail

# Configuration
FUNCTION_NAME="personal-website-api"
REGION="ap-southeast-2"

EXE_FILE="bootstrap"
ZIP_FILE_NAME="$FUNCTION_NAME.zip"

echo "Building application"
GOOS=linux GOARCH=amd64 go build -tags lambda.norpc -o bootstrap ./cmd

echo "Zipping file"
zip $ZIP_FILE_NAME $EXE_FILE

echo "Updating Lambda function..."
aws lambda update-function-code \
	--function-name $FUNCTION_NAME \
	--zip-file "fileb://$ZIP_FILE_NAME" \
	--region $REGION

rm $ZIP_FILE_NAME
rm $EXE_FILE
