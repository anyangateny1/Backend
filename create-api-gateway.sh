#!/bin/bash

# Configuration
API_NAME="personal-website-api"
REGION="ap-southeast-2"
LAMBDA_FUNCTION_NAME="personal-website-backend"
STAGE_NAME="prod"

# Create the API
echo "Creating API Gateway..."
API_ID=$(aws apigateway create-rest-api \
    --name "$API_NAME" \
    --region "$REGION" \
    --query 'id' \
    --output text)

echo "API Gateway created with ID: $API_ID"

# Get the root resource ID
ROOT_RESOURCE_ID=$(aws apigateway get-resources \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --query 'items[0].id' \
    --output text)

# Create /api resource
echo "Creating /api resource..."
API_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --parent-id "$ROOT_RESOURCE_ID" \
    --path-part "api" \
    --query 'id' \
    --output text)

# Create /api/projects resource and method
echo "Creating /api/projects endpoint..."
PROJECTS_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --parent-id "$API_RESOURCE_ID" \
    --path-part "projects" \
    --query 'id' \
    --output text)

# Create GET method for /api/projects
aws apigateway put-method \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --resource-id "$PROJECTS_RESOURCE_ID" \
    --http-method "GET" \
    --authorization-type "NONE"

# Create /api/contact resource and method
echo "Creating /api/contact endpoint..."
CONTACT_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --parent-id "$API_RESOURCE_ID" \
    --path-part "contact" \
    --query 'id' \
    --output text)

# Create POST method for /api/contact
aws apigateway put-method \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --resource-id "$CONTACT_RESOURCE_ID" \
    --http-method "POST" \
    --authorization-type "NONE"

# Create /api/images resource and method
echo "Creating /api/images endpoint..."
IMAGES_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --parent-id "$API_RESOURCE_ID" \
    --path-part "images" \
    --query 'id' \
    --output text)

# Create {imageName} resource under /api/images
IMAGE_NAME_RESOURCE_ID=$(aws apigateway create-resource \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --parent-id "$IMAGES_RESOURCE_ID" \
    --path-part "{imageName}" \
    --query 'id' \
    --output text)

# Create GET method for /api/images/{imageName}
aws apigateway put-method \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --resource-id "$IMAGE_NAME_RESOURCE_ID" \
    --http-method "GET" \
    --authorization-type "NONE" \
    --request-parameters "method.request.path.imageName=true"

# Get Lambda function ARN
LAMBDA_ARN=$(aws lambda get-function \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --region "$REGION" \
    --query 'Configuration.FunctionArn' \
    --output text)

# Function to create Lambda integration
create_lambda_integration() {
    local RESOURCE_ID=$1
    local HTTP_METHOD=$2
    
    # Create integration
    aws apigateway put-integration \
        --rest-api-id "$API_ID" \
        --region "$REGION" \
        --resource-id "$RESOURCE_ID" \
        --http-method "$HTTP_METHOD" \
        --type "AWS_PROXY" \
        --integration-http-method "POST" \
        --uri "arn:aws:apigateway:$REGION:lambda:path/2015-03-31/functions/$LAMBDA_ARN/invocations"
}

# Create Lambda integrations for all endpoints
echo "Creating Lambda integrations..."
create_lambda_integration "$PROJECTS_RESOURCE_ID" "GET"
create_lambda_integration "$CONTACT_RESOURCE_ID" "POST"
create_lambda_integration "$IMAGE_NAME_RESOURCE_ID" "GET"

# Add Lambda permission for API Gateway
echo "Adding Lambda permissions..."
aws lambda add-permission \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --statement-id "AllowAPIGatewayInvoke" \
    --action "lambda:InvokeFunction" \
    --principal "apigateway.amazonaws.com" \
    --source-arn "arn:aws:execute-api:$REGION:$(aws sts get-caller-identity --query 'Account' --output text):$API_ID/*/*/*"

# Enable CORS for all resources
enable_cors() {
    local RESOURCE_ID=$1
    
    # Add OPTIONS method
    aws apigateway put-method \
        --rest-api-id "$API_ID" \
        --region "$REGION" \
        --resource-id "$RESOURCE_ID" \
        --http-method "OPTIONS" \
        --authorization-type "NONE"
    
    # Add OPTIONS integration
    aws apigateway put-integration \
        --rest-api-id "$API_ID" \
        --region "$REGION" \
        --resource-id "$RESOURCE_ID" \
        --http-method "OPTIONS" \
        --type "MOCK" \
        --request-templates '{"application/json":"{\"statusCode\": 200}"}'
    
    # Add method response FIRST
    aws apigateway put-method-response \
        --rest-api-id "$API_ID" \
        --region "$REGION" \
        --resource-id "$RESOURCE_ID" \
        --http-method "OPTIONS" \
        --status-code "200" \
        --response-parameters "{\"method.response.header.Access-Control-Allow-Headers\":true,\"method.response.header.Access-Control-Allow-Methods\":true,\"method.response.header.Access-Control-Allow-Origin\":true}"
    
    # Then add integration response
    aws apigateway put-integration-response \
        --rest-api-id "$API_ID" \
        --region "$REGION" \
        --resource-id "$RESOURCE_ID" \
        --http-method "OPTIONS" \
        --status-code "200" \
        --response-parameters "{\"method.response.header.Access-Control-Allow-Headers\":\"'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'\",\"method.response.header.Access-Control-Allow-Methods\":\"'GET,POST,OPTIONS'\",\"method.response.header.Access-Control-Allow-Origin\":\"'*'\"}"
}

echo "Enabling CORS..."
enable_cors "$PROJECTS_RESOURCE_ID"
enable_cors "$CONTACT_RESOURCE_ID"
enable_cors "$IMAGE_NAME_RESOURCE_ID"

# Deploy the API
echo "Deploying API..."
DEPLOYMENT_ID=$(aws apigateway create-deployment \
    --rest-api-id "$API_ID" \
    --region "$REGION" \
    --stage-name "$STAGE_NAME" \
    --query 'id' \
    --output text)

# Get the API endpoint
API_ENDPOINT="https://$API_ID.execute-api.$REGION.amazonaws.com/$STAGE_NAME"
echo "API Gateway deployment completed!"
echo "Your API endpoint is: $API_ENDPOINT"
echo "Use this endpoint in your frontend application" 