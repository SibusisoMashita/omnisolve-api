# Initial Setup Checklist

Complete these steps before your first deployment to production.

## 1. GitHub Repository Setup

- [ ] Create GitHub repository
- [ ] Push initial code to `main` branch
- [ ] Set up branch protection rules for `main`
- [ ] Enable GitHub Actions

## 2. AWS Account Setup

- [ ] Create AWS account (if not exists)
- [ ] Create IAM user for CI/CD with programmatic access
- [ ] Attach required policies to IAM user:
  - `AmazonRDSFullAccess`
  - `AmazonS3FullAccess`
  - `AmazonEC2FullAccess`
  - `IAMReadOnlyAccess`
- [ ] Save Access Key ID and Secret Access Key

## 3. GitHub Secrets Configuration

Navigate to: `Settings` → `Secrets and variables` → `Actions` → `New repository secret`

Add the following secrets:

| Secret Name | Description | Example Value |
|-------------|-------------|---------------|
| `AWS_ACCESS_KEY_ID` | AWS IAM user access key | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM user secret key | `wJalrXUtn...` |
| `DOCKER_USERNAME` | Docker Hub username | `your-username` |
| `DOCKER_PASSWORD` | Docker Hub password/token | `dckr_pat_...` |

## 4. Terraform Backend Setup

The infrastructure workflow will automatically use the S3 backend configured in `infrastructure/terraform/main.tf`.

Ensure the following resources exist (or will be created on first run):
- S3 bucket: `omnisolve-terraform-state`
- DynamoDB table: `omnisolve-terraform-lock`

## 5. Terraform Variables

Update `infrastructure/terraform/terraform.tfvars` with your values:

```hcl
project_name         = "omnisolve"
environment          = "prod"
aws_region           = "us-east-1"

vpc_id               = "vpc-xxxxx"           # Your VPC ID
private_subnet_ids   = ["subnet-xxx", "subnet-yyy"]  # At least 2 subnets in different AZs
allowed_cidr_blocks  = ["10.0.0.0/16"]       # Your VPC CIDR

db_name              = "omnisolve"
db_username          = "omnisolve"
db_password          = "CHANGE-ME-SECURE-PASSWORD"  # Use strong password
db_instance_class    = "db.t3.micro"
db_allocated_storage = 20
s3_bucket_name       = "omnisolve-documents-prod-YOUR-ACCOUNT-ID"
```

**Important:** Never commit `terraform.tfvars` with real credentials to Git. Add it to `.gitignore`.

## 6. First Deployment

### Option A: Automatic (Recommended)

1. Push code to `main` branch
2. GitHub Actions will automatically:
   - Run tests
   - Build Docker image
   - Deploy infrastructure (if Terraform files changed)

### Option B: Manual Infrastructure Deployment

1. Go to `Actions` tab in GitHub
2. Select `Deploy Infrastructure` workflow
3. Click `Run workflow`
4. Choose `apply` action
5. Click `Run workflow` button

## 7. Verify Deployment

After successful deployment, check:

- [ ] GitHub Actions workflow completed successfully
- [ ] AWS RDS PostgreSQL instance is running
- [ ] AWS S3 bucket is created
- [ ] Security groups are configured correctly
- [ ] Application can connect to database

## 8. Post-Deployment Configuration

- [ ] Update application environment variables with RDS endpoint
- [ ] Configure DNS (if applicable)
- [ ] Set up monitoring and alerts
- [ ] Configure backup policies
- [ ] Review security group rules

## 9. Enable Authentication (After Testing)

Once testing is complete, re-enable JWT authentication:

1. Restore `@SecurityRequirement(name = "bearerAuth")` annotations in controllers
2. Restore `@AuthenticationPrincipal Jwt jwt` parameters
3. Set `JWT_ENABLED=true` in application configuration
4. Configure AWS Cognito User Pool
5. Update `COGNITO_ISSUER_URI` and `COGNITO_AUDIENCE` environment variables

## Troubleshooting

### Terraform State Lock Issues

If you encounter state lock errors:

```bash
# Check DynamoDB for locks
aws dynamodb scan --table-name omnisolve-terraform-lock --region us-east-1

# Force unlock (use with caution)
terraform force-unlock LOCK_ID
```

### RDS Connection Issues

- Verify security group allows traffic from your application
- Check VPC subnet configuration
- Ensure RDS is in "Available" state
- Verify database credentials

### GitHub Actions Failures

- Check workflow logs in Actions tab
- Verify all secrets are configured correctly
- Ensure IAM user has required permissions
- Check AWS service quotas

## Support

For issues or questions:
- Check GitHub Actions logs
- Review AWS CloudWatch logs
- Consult Terraform documentation
- Contact DevOps team
