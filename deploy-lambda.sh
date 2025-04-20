#!/bin/bash

# Configuration
FUNCTION_NAME="personal-website-backend"
REGION="ap-southeast-2"  # Changed to Sydney region
HANDLER="com.example.LambdaHandler::handleRequest"
MEMORY_SIZE=512
TIMEOUT=30

# Build the application
echo "Building the application..."
mvn clean package

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

JAR_FILE="target/personal-website-backend-1.0-SNAPSHOT.jar"

# Check if function exists
if aws lambda get-function --function-name $FUNCTION_NAME --region $REGION 2>&1 | grep -q "Function not found"; then
    # Create new function
    echo "Creating new Lambda function..."
    aws lambda create-function \
        --function-name $FUNCTION_NAME \
        --runtime java11 \
        --handler $HANDLER \
        --memory-size $MEMORY_SIZE \
        --timeout $TIMEOUT \
        --role "arn:aws:iam::739275456268:role/website-lambda" \
        --zip-file fileb://$JAR_FILE \
        --region $REGION
else
    # Update existing function
    echo "Updating existing Lambda function..."
    aws lambda update-function-code \
        --function-name $FUNCTION_NAME \
        --zip-file fileb://$JAR_FILE \
        --region $REGION
fi

# Update function configuration
echo "Updating function configuration..."
aws lambda update-function-configuration \
    --function-name $FUNCTION_NAME \
    --handler $HANDLER \
    --runtime java11 \
    --memory-size $MEMORY_SIZE \
    --timeout $TIMEOUT \
    --region $REGION

echo "Deployment completed!"
echo "Note: Make sure to:"
echo "1. Set up an API Gateway trigger for your Lambda function"
echo "2. Configure the necessary IAM roles and permissions"
echo "3. Update the frontend API endpoint to point to your API Gateway URL" 