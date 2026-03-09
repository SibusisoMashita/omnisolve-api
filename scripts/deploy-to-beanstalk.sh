#!/bin/bash

# OmniSolve Elastic Beanstalk Deployment Script
# Usage: ./scripts/deploy-to-beanstalk.sh [dev|prod] [version-label]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check arguments
if [ $# -lt 1 ]; then
    echo -e "${RED}Error: Environment not specified${NC}"
    echo "Usage: $0 [dev|prod] [version-label]"
    exit 1
fi

ENVIRONMENT=$1
VERSION_LABEL=${2:-$(date +%Y%m%d-%H%M%S)}

# Validate environment
if [ "$ENVIRONMENT" != "dev" ] && [ "$ENVIRONMENT" != "prod" ]; then
    echo -e "${RED}Error: Environment must be 'dev' or 'prod'${NC}"
    exit 1
fi

echo -e "${GREEN}=== OmniSolve Deployment ===${NC}"
echo "Environment: $ENVIRONMENT"
echo "Version: $VERSION_LABEL"
echo ""

# Get Terraform outputs
echo -e "${YELLOW}Getting infrastructure details...${NC}"
cd "infrastructure/$ENVIRONMENT"

APP_NAME=$(terraform output -raw beanstalk_application_name 2>/dev/null)
ENV_NAME=$(terraform output -raw beanstalk_environment_name 2>/dev/null)
DEPLOY_BUCKET=$(terraform output -raw deployments_bucket_name 2>/dev/null)

if [ -z "$APP_NAME" ] || [ -z "$ENV_NAME" ] || [ -z "$DEPLOY_BUCKET" ]; then
    echo -e "${RED}Error: Could not get Terraform outputs. Has infrastructure been deployed?${NC}"
    exit 1
fi

echo "Application: $APP_NAME"
echo "Environment: $ENV_NAME"
echo "Bucket: $DEPLOY_BUCKET"
echo ""

cd ../..

# Build application
echo -e "${YELLOW}Building application...${NC}"
./mvnw clean package -DskipTests

if [ ! -f "target/omnisolve-api.jar" ]; then
    echo -e "${RED}Error: JAR file not found after build${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Build successful${NC}"
echo ""

# Upload to S3
echo -e "${YELLOW}Uploading to S3...${NC}"
JAR_KEY="omnisolve-api-${VERSION_LABEL}.jar"
aws s3 cp target/omnisolve-api.jar "s3://${DEPLOY_BUCKET}/${JAR_KEY}"

echo -e "${GREEN}✓ Upload successful${NC}"
echo ""

# Create application version
echo -e "${YELLOW}Creating application version...${NC}"
aws elasticbeanstalk create-application-version \
    --application-name "$APP_NAME" \
    --version-label "$VERSION_LABEL" \
    --source-bundle S3Bucket="$DEPLOY_BUCKET",S3Key="$JAR_KEY" \
    --description "Deployed via script on $(date)" \
    --no-auto-create-application

echo -e "${GREEN}✓ Application version created${NC}"
echo ""

# Deploy to environment
echo -e "${YELLOW}Deploying to environment...${NC}"
aws elasticbeanstalk update-environment \
    --application-name "$APP_NAME" \
    --environment-name "$ENV_NAME" \
    --version-label "$VERSION_LABEL"

echo -e "${GREEN}✓ Deployment initiated${NC}"
echo ""

# Wait for deployment
echo -e "${YELLOW}Waiting for deployment to complete...${NC}"
echo "This may take several minutes..."

aws elasticbeanstalk wait environment-updated \
    --environment-names "$ENV_NAME" \
    --no-cli-pager

echo -e "${GREEN}✓ Deployment complete${NC}"
echo ""

# Get environment URL
ENV_URL=$(aws elasticbeanstalk describe-environments \
    --environment-names "$ENV_NAME" \
    --query 'Environments[0].CNAME' \
    --output text)

echo -e "${GREEN}=== Deployment Successful ===${NC}"
echo "Environment URL: http://$ENV_URL"
echo "Health Check: http://$ENV_URL/api/health"
echo ""

# Test health endpoint
echo -e "${YELLOW}Testing health endpoint...${NC}"
sleep 10  # Wait for application to start

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://$ENV_URL/api/health" || echo "000")

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✓ Health check passed!${NC}"
    echo ""
    echo -e "${GREEN}🚀 Deployment completed successfully!${NC}"
else
    echo -e "${YELLOW}⚠ Health check returned: $HTTP_CODE${NC}"
    echo "The application may still be starting up."
    echo "Check the environment status in AWS Console or run:"
    echo "  aws elasticbeanstalk describe-environments --environment-names $ENV_NAME"
fi

echo ""
echo "To view logs:"
echo "  aws elasticbeanstalk retrieve-environment-info --environment-name $ENV_NAME --info-type tail"
