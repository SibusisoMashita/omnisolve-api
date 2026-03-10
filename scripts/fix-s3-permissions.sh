#!/bin/bash
set -e

echo "=== Fixing S3 Permissions for Document Uploads ==="
echo ""

# Configuration
ROLE_NAME="prod-omnisolve-beanstalk-ec2-role"
POLICY_NAME="OmnisolveS3Access"
DEV_BUCKET="dev-omnisolve-documents"
PROD_BUCKET="prod-omnisolve-documents"
EB_ENV_NAME="prod-omnisolve-api"
AWS_REGION="us-east-1"

echo "Step 1: Creating IAM policy for S3 access..."

# Create temporary policy file
cat > /tmp/s3-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "OmnisolveS3DocumentAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::${DEV_BUCKET}/*",
        "arn:aws:s3:::${DEV_BUCKET}",
        "arn:aws:s3:::${PROD_BUCKET}/*",
        "arn:aws:s3:::${PROD_BUCKET}"
      ]
    }
  ]
}
EOF

# Attach policy to role
echo "Attaching policy to role: ${ROLE_NAME}..."
aws iam put-role-policy \
  --role-name "${ROLE_NAME}" \
  --policy-name "${POLICY_NAME}" \
  --policy-document file:///tmp/s3-policy.json

echo "✓ IAM policy attached successfully"
echo ""

echo "Step 2: Creating production S3 bucket..."

# Check if bucket exists
if aws s3 ls "s3://${PROD_BUCKET}" 2>/dev/null; then
  echo "✓ Bucket ${PROD_BUCKET} already exists"
else
  # Create bucket
  aws s3api create-bucket \
    --bucket "${PROD_BUCKET}" \
    --region "${AWS_REGION}"
  
  echo "✓ Bucket ${PROD_BUCKET} created"
  
  # Enable versioning
  aws s3api put-bucket-versioning \
    --bucket "${PROD_BUCKET}" \
    --versioning-configuration Status=Enabled
  
  echo "✓ Versioning enabled"
  
  # Block public access
  aws s3api put-public-access-block \
    --bucket "${PROD_BUCKET}" \
    --public-access-block-configuration \
      "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
  
  echo "✓ Public access blocked"
  
  # Add encryption
  aws s3api put-bucket-encryption \
    --bucket "${PROD_BUCKET}" \
    --server-side-encryption-configuration '{
      "Rules": [{
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        }
      }]
    }'
  
  echo "✓ Encryption enabled"
fi

echo ""
echo "Step 3: Updating Elastic Beanstalk environment variable..."

# Update environment variable
aws elasticbeanstalk update-environment \
  --region "${AWS_REGION}" \
  --environment-name "${EB_ENV_NAME}" \
  --option-settings \
    Namespace=aws:elasticbeanstalk:application:environment,OptionName=APP_S3_BUCKET,Value="${PROD_BUCKET}"

echo "✓ Environment variable updated to: ${PROD_BUCKET}"
echo ""

echo "=== All Done! ==="
echo ""
echo "Summary:"
echo "  ✓ IAM permissions added to ${ROLE_NAME}"
echo "  ✓ Production bucket ${PROD_BUCKET} ready"
echo "  ✓ Environment variable updated"
echo ""
echo "The Elastic Beanstalk environment will restart automatically."
echo "Wait 2-3 minutes, then test document upload at https://omnisolve.africa/"
echo ""
echo "To verify the changes:"
echo "  aws iam get-role-policy --role-name ${ROLE_NAME} --policy-name ${POLICY_NAME}"
echo "  aws elasticbeanstalk describe-configuration-settings --environment-name ${EB_ENV_NAME} | grep APP_S3_BUCKET"
echo ""

# Cleanup
rm /tmp/s3-policy.json
