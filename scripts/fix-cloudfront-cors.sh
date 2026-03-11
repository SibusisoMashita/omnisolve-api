#!/bin/bash
set -e

echo "=== Fixing CloudFront CORS Configuration ==="
echo ""

# Configuration
DISTRIBUTION_ID="E21RX7XGSJBV4Z"  # d3s7bt9q3x42ay.cloudfront.net
REGION="us-east-1"

echo "Step 1: Getting current CloudFront distribution configuration..."

# Get current config
aws cloudfront get-distribution-config \
  --id "${DISTRIBUTION_ID}" \
  --region "${REGION}" \
  > /tmp/cf-config.json

# Extract ETag for update
ETAG=$(jq -r '.ETag' /tmp/cf-config.json)
echo "Current ETag: ${ETAG}"

# Extract just the DistributionConfig
jq '.DistributionConfig' /tmp/cf-config.json > /tmp/cf-dist-config.json

echo ""
echo "Step 2: Updating cache behavior to forward CORS headers..."

# Update the default cache behavior to forward Origin header and cache based on it
jq '.DefaultCacheBehavior.ForwardedValues.Headers.Quantity = 3 |
    .DefaultCacheBehavior.ForwardedValues.Headers.Items = ["Origin", "Access-Control-Request-Headers", "Access-Control-Request-Method"] |
    .DefaultCacheBehavior.AllowedMethods.Quantity = 7 |
    .DefaultCacheBehavior.AllowedMethods.Items = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"] |
    .DefaultCacheBehavior.AllowedMethods.CachedMethods.Quantity = 3 |
    .DefaultCacheBehavior.AllowedMethods.CachedMethods.Items = ["GET", "HEAD", "OPTIONS"]' \
    /tmp/cf-dist-config.json > /tmp/cf-dist-config-updated.json

echo "✓ Configuration updated"
echo ""
echo "Step 3: Applying changes to CloudFront distribution..."

# Update the distribution
aws cloudfront update-distribution \
  --id "${DISTRIBUTION_ID}" \
  --distribution-config file:///tmp/cf-dist-config-updated.json \
  --if-match "${ETAG}" \
  --region "${REGION}" \
  > /tmp/cf-update-result.json

echo "✓ CloudFront distribution updated"
echo ""
echo "Step 4: Creating cache invalidation to clear old cached responses..."

# Create invalidation
INVALIDATION_ID=$(aws cloudfront create-invalidation \
  --distribution-id "${DISTRIBUTION_ID}" \
  --paths "/*" \
  --region "${REGION}" \
  --query 'Invalidation.Id' \
  --output text)

echo "✓ Invalidation created: ${INVALIDATION_ID}"
echo ""

echo "=== CloudFront CORS Fix Complete ==="
echo ""
echo "Summary:"
echo "  ✓ CloudFront now forwards Origin header to backend"
echo "  ✓ CloudFront caches based on Origin header"
echo "  ✓ All HTTP methods enabled (GET, POST, PUT, DELETE, OPTIONS, PATCH)"
echo "  ✓ Cache invalidation in progress"
echo ""
echo "Wait 5-10 minutes for:"
echo "  1. CloudFront configuration to propagate globally"
echo "  2. Cache invalidation to complete"
echo ""
echo "Then test at: https://omnisolve.africa/dashboard"
echo ""
echo "To check invalidation status:"
echo "  aws cloudfront get-invalidation --distribution-id ${DISTRIBUTION_ID} --id ${INVALIDATION_ID}"
echo ""

# Cleanup
rm -f /tmp/cf-config.json /tmp/cf-dist-config.json /tmp/cf-dist-config-updated.json /tmp/cf-update-result.json
