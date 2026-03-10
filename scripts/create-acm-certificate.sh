#!/bin/bash

# Script to request an ACM certificate for your domain
# Usage: ./create-acm-certificate.sh api.yourdomain.com

set -e

DOMAIN=$1
REGION="us-east-1"

if [ -z "$DOMAIN" ]; then
    echo "Usage: $0 <domain-name>"
    echo "Example: $0 api.yourdomain.com"
    exit 1
fi

echo "Requesting ACM certificate for domain: $DOMAIN"
echo "Region: $REGION"
echo ""

# Request certificate
CERT_ARN=$(aws acm request-certificate \
    --domain-name "$DOMAIN" \
    --validation-method DNS \
    --region "$REGION" \
    --query 'CertificateArn' \
    --output text)

echo "Certificate requested successfully!"
echo "Certificate ARN: $CERT_ARN"
echo ""

# Wait a moment for AWS to generate validation records
echo "Waiting for validation records..."
sleep 5

# Get validation records
echo ""
echo "DNS Validation Records:"
echo "======================="
aws acm describe-certificate \
    --certificate-arn "$CERT_ARN" \
    --region "$REGION" \
    --query 'Certificate.DomainValidationOptions[0].ResourceRecord' \
    --output table

echo ""
echo "Next steps:"
echo "1. Add the CNAME record shown above to your DNS provider"
echo "2. Wait for validation (5-30 minutes)"
echo "3. Check status with: aws acm describe-certificate --certificate-arn $CERT_ARN --region $REGION"
echo "4. Once validated, add this ARN to your terraform.tfvars:"
echo "   ssl_certificate_arn = \"$CERT_ARN\""
