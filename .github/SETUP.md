# GitHub Repository Setup Guide

This guide walks you through setting up the OmniSolve API repository with automated CI/CD pipelines.

## Prerequisites

- GitHub repository created
- AWS account with appropriate permissions
- Terraform state backend (S3 bucket + DynamoDB table)

## Step 1: Create GitHub Repository Secrets

Navigate to your repository settings: `Settings > Secrets and variables > Actions`

Add the following secrets:

### Required Secrets

| Secret Name | Description | Example Value |
|-------------|-------------|---------------|
| `AWS_ACCESS_KEY_ID` | AWS IAM access key | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret key | `wJalrXUtn...` |

### Optional Secrets (for production)

| Secret Name | Description |
|-------------|-------------|
| `DB_PASSWORD` | Production database password |
| `COGNITO_ISSUER_URI` | AWS Cognito issuer URI |
| `COGNITO_AUDIENCE` | AWS Cognito client ID |

## Step 2: Create GitHub Environment

1. Go to `Settings > Environments`
2. Create a new environment named `production`
3. Add protection rules:
   - ✅ Required reviewers (recommended)
   - ✅ Wait timer (optional)
   - ✅ Deployment branches: `main` only

## Step 3: Verify Terraform Backend

Ensure the following AWS resources exist:

```bash
# S3 bucket for Terraform state
aws s3 ls s3://omnisolve-terraform-state

# DynamoDB table for state locking
aws dynamodb describe-table --table-name omnisolve-terraform-lock
```

If they don't exist, create them:

```bash
# Create S3 bucket
aws s3api create-bucket \
  --bucket omnisolve-terraform-state \
  --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket omnisolve-terraform-state \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket omnisolve-terraform-state \
  --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

# Create DynamoDB table
aws dynamodb create-table \
  --table-name omnisolve-terraform-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## Step 4: Update Terraform Variables for Production

Create `infrastructure/terraform/terraform.tfvars` for production (DO NOT commit this file):

```hcl
project_name         = "omnisolve"
environment          = "prod"
aws_region           = "us-east-1"

vpc_id               = "vpc-YOUR_VPC_ID"
private_subnet_ids   = ["subnet-XXXXX", "subnet-YYYYY"]
allowed_cidr_blocks  = ["10.0.0.0/16"]

db_name              = "omnisolve"
db_username          = "omnisolve"
db_password          = "SECURE_PASSWORD_HERE"
db_instance_class    = "db.t3.small"
db_allocated_storage = 50
s3_bucket_name       = "omnisolve-documents-prod"
```

## Step 5: Workflows Overview

### 🔍 Terraform Plan (Pull Requests)

**Trigger**: Pull requests to `main` with Terraform changes

**Actions**:
- Validates Terraform configuration
- Runs `terraform plan`
- Posts plan output as PR comment

**File**: `.github/workflows/terraform-plan.yml`

### 🚀 Terraform Apply (Main Branch)

**Trigger**: Push to `main` with Terraform changes

**Actions**:
- Provisions AWS infrastructure
- Creates RDS PostgreSQL database
- Creates S3 bucket for documents
- Outputs connection details

**File**: `.github/workflows/terraform-apply.yml`

### 🏗️ CI Build and Test

**Trigger**: Pull requests and pushes to `main`

**Actions**:
- Builds Java application with Maven
- Runs unit tests with PostgreSQL
- Uploads build artifacts

**File**: `.github/workflows/ci.yml`

### 🐳 Docker Build and Push

**Trigger**: Push to `main` or version tags

**Actions**:
- Builds Docker image
- Pushes to GitHub Container Registry
- Tags with version and latest

**File**: `.github/workflows/docker-build.yml`

## Step 6: Initial Repository Setup

```bash
# Initialize git repository
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit: OmniSolve API with Terraform infrastructure"

# Add remote (replace with your repository URL)
git remote add origin https://github.com/YOUR_USERNAME/omnisolve-api.git

# Push to main
git push -u origin main
```

## Step 7: Workflow Execution Order

When you merge a PR to `main`:

1. **Terraform Apply** runs first (if Terraform files changed)
   - Provisions production infrastructure
   - Outputs database endpoint and S3 bucket

2. **CI Build** runs in parallel
   - Builds and tests the application

3. **Docker Build** runs after CI passes
   - Creates container image
   - Pushes to registry

## Step 8: Verify Deployment

After workflows complete:

```bash
# Check Terraform outputs
cd infrastructure/terraform
terraform output

# Verify RDS instance
aws rds describe-db-instances \
  --db-instance-identifier omnisolve-prod-postgres

# Verify S3 bucket
aws s3 ls s3://omnisolve-documents-prod
```

## Troubleshooting

### Terraform State Lock Issues

If you encounter state lock errors:

```bash
# List locks
aws dynamodb scan --table-name omnisolve-terraform-lock

# Force unlock (use with caution)
terraform force-unlock LOCK_ID
```

### AWS Credentials Issues

Verify your IAM user has these permissions:
- `AmazonRDSFullAccess`
- `AmazonS3FullAccess`
- `AmazonEC2FullAccess` (for VPC/security groups)
- `AmazonDynamoDBFullAccess` (for state locking)

### Workflow Failures

Check the Actions tab in GitHub for detailed logs and error messages.

## Security Best Practices

1. ✅ Never commit `terraform.tfvars` with sensitive data
2. ✅ Use GitHub Secrets for all credentials
3. ✅ Enable branch protection on `main`
4. ✅ Require PR reviews before merging
5. ✅ Use GitHub Environment protection for production
6. ✅ Rotate AWS credentials regularly
7. ✅ Consider using AWS Secrets Manager for database passwords

## Cost Optimization

Current configuration uses:
- `db.t3.micro` for dev (free tier eligible)
- `db.t3.small` for prod (low cost)
- Single-AZ deployment (no multi-AZ cost)
- No Performance Insights (saves ~$7/month)
- 7-day backup retention (minimal storage cost)

Estimated monthly cost: ~$15-30 for dev, ~$30-50 for prod
