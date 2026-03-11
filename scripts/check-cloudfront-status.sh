#!/bin/bash

DISTRIBUTION_ID="E21RX7XGSJBV4Z"
INVALIDATION_ID="I1LW9IYDRAVGK907Q5G3NRI6Z"

echo "=== CloudFront Status Check ==="
echo ""

echo "1. Distribution Status:"
aws cloudfront get-distribution \
  --id "${DISTRIBUTION_ID}" \
  --query 'Distribution.{Status:Status,DomainName:DomainName,LastModified:LastModifiedTime}' \
  --output table

echo ""
echo "2. Invalidation Status:"
aws cloudfront get-invalidation \
  --distribution-id "${DISTRIBUTION_ID}" \
  --id "${INVALIDATION_ID}" \
  --query 'Invalidation.{Status:Status,CreateTime:CreateTime}' \
  --output table

echo ""
echo "3. Cache Behavior Configuration:"
aws cloudfront get-distribution-config \
  --id "${DISTRIBUTION_ID}" \
  --query 'DistributionConfig.DefaultCacheBehavior.{ForwardedHeaders:ForwardedValues.Headers.Items,AllowedMethods:AllowedMethods.Items}' \
  --output json

echo ""
echo "Status Guide:"
echo "  - Distribution Status: Should be 'Deployed'"
echo "  - Invalidation Status: 'InProgress' → 'Completed' (takes 5-10 minutes)"
echo ""
echo "Once invalidation is 'Completed', test at:"
echo "  https://omnisolve.africa/dashboard"
echo ""
