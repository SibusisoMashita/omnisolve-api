#!/bin/bash

# Quick script to check for recent errors in production logs

LOG_GROUP="/aws/elasticbeanstalk/prod-omnisolve-api/var/log/web.stdout.log"
REGION="us-east-1"
MINUTES=${1:-30}

echo "=== Checking for errors in last ${MINUTES} minutes ==="
echo ""

# Check for ERROR level logs
echo "1. ERROR level logs:"
aws logs tail "${LOG_GROUP}" \
    --region "${REGION}" \
    --since ${MINUTES}m \
    --filter-pattern "ERROR" \
    --format short \
    2>/dev/null || echo "No errors found or log group not accessible"

echo ""
echo "---"
echo ""

# Check for failed uploads
echo "2. Failed document uploads:"
aws logs tail "${LOG_GROUP}" \
    --region "${REGION}" \
    --since ${MINUTES}m \
    --filter-pattern "Upload validation failed" \
    --format short \
    2>/dev/null || echo "No failed uploads"

echo ""
echo "---"
echo ""

# Check for S3 errors
echo "3. S3 upload failures:"
aws logs tail "${LOG_GROUP}" \
    --region "${REGION}" \
    --since ${MINUTES}m \
    --filter-pattern "S3 upload failed" \
    --format short \
    2>/dev/null || echo "No S3 failures"

echo ""
echo "---"
echo ""

# Check for invalid state transitions
echo "4. Invalid state transitions:"
aws logs tail "${LOG_GROUP}" \
    --region "${REGION}" \
    --since ${MINUTES}m \
    --filter-pattern "Invalid state transition" \
    --format short \
    2>/dev/null || echo "No invalid transitions"

echo ""
echo "=== Summary ==="
echo "Checked logs from last ${MINUTES} minutes"
echo "To check a different time period: $0 <minutes>"
echo ""
