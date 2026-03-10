#!/bin/bash

echo "=== Verifying S3 Upload Fix ==="
echo ""

ROLE_NAME="prod-omnisolve-beanstalk-ec2-role"
POLICY_NAME="OmnisolveS3Access"
PROD_BUCKET="prod-omnisolve-documents"
EB_ENV_NAME="prod-omnisolve-api"

echo "1. Checking IAM Policy..."
if aws iam get-role-policy --role-name "${ROLE_NAME}" --policy-name "${POLICY_NAME}" &>/dev/null; then
  echo "✓ IAM policy exists"
  echo ""
  echo "Policy details:"
  aws iam get-role-policy --role-name "${ROLE_NAME}" --policy-name "${POLICY_NAME}" --query 'PolicyDocument' --output json
else
  echo "✗ IAM policy not found"
fi

echo ""
echo "2. Checking S3 Bucket..."
if aws s3 ls "s3://${PROD_BUCKET}" &>/dev/null; then
  echo "✓ Bucket ${PROD_BUCKET} exists and is accessible"
  
  # Check versioning
  VERSIONING=$(aws s3api get-bucket-versioning --bucket "${PROD_BUCKET}" --query 'Status' --output text)
  echo "  Versioning: ${VERSIONING}"
  
  # Check encryption
  if aws s3api get-bucket-encryption --bucket "${PROD_BUCKET}" &>/dev/null; then
    echo "  Encryption: Enabled"
  else
    echo "  Encryption: Not configured"
  fi
else
  echo "✗ Bucket ${PROD_BUCKET} not found or not accessible"
fi

echo ""
echo "3. Checking Elastic Beanstalk Environment Variable..."
BUCKET_VAR=$(aws elasticbeanstalk describe-configuration-settings \
  --environment-name "${EB_ENV_NAME}" \
  --query "ConfigurationSettings[0].OptionSettings[?OptionName=='APP_S3_BUCKET'].Value" \
  --output text)

if [ -n "${BUCKET_VAR}" ]; then
  echo "✓ APP_S3_BUCKET = ${BUCKET_VAR}"
  
  if [ "${BUCKET_VAR}" = "${PROD_BUCKET}" ]; then
    echo "  ✓ Correctly set to production bucket"
  else
    echo "  ⚠ Warning: Not set to ${PROD_BUCKET}"
  fi
else
  echo "✗ APP_S3_BUCKET not found in environment"
fi

echo ""
echo "4. Checking Environment Health..."
ENV_STATUS=$(aws elasticbeanstalk describe-environments \
  --environment-names "${EB_ENV_NAME}" \
  --query "Environments[0].Status" \
  --output text)

ENV_HEALTH=$(aws elasticbeanstalk describe-environments \
  --environment-names "${EB_ENV_NAME}" \
  --query "Environments[0].Health" \
  --output text)

echo "  Status: ${ENV_STATUS}"
echo "  Health: ${ENV_HEALTH}"

if [ "${ENV_STATUS}" = "Ready" ]; then
  echo "  ✓ Environment is ready"
else
  echo "  ⚠ Environment is updating (wait a few minutes)"
fi

echo ""
echo "=== Verification Complete ==="
echo ""
echo "Next steps:"
echo "  1. Wait for environment to be 'Ready' if it's updating"
echo "  2. Test document upload at https://omnisolve.africa/"
echo "  3. Check CloudWatch logs: https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/omnisolve-prod"
echo ""
