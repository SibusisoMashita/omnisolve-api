# Deployment Guide

Complete guide for deploying OmniSolve API to production.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Initial Setup](#initial-setup)
- [Local Development](#local-development)
- [Production Deployment](#production-deployment)
- [Post-Deployment](#post-deployment)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools
- Git
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Terraform 1.6+
- AWS CLI v2

### Required Accounts
- GitHub account with repository access
- AWS account with appropriate permissions
- Docker Hub or GitHub Container Registry access

---

## Initial Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/omnisolve-api.git
cd omnisolve-api
```

### 2. Configure AWS Credentials

```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Default region: us-east-1
# Default output format: json
```

### 3. Set Up GitHub Secrets

Navigate to: `Settings` → `Secrets and variables` → `Actions`

Add these secrets:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

### 4. Configure Terraform Variables

```bash
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
project_name         = "omnisolve"
environment          = "prod"
aws_region           = "us-east-1"

# Get your VPC ID
vpc_id               = "vpc-xxxxx"

# Get subnet IDs (must be in 2+ different AZs)
private_subnet_ids   = ["subnet-xxxxx", "subnet-yyyyy"]

# Your VPC CIDR block
allowed_cidr_blocks  = ["172.31.0.0/16"]

# Database configuration
db_name              = "omnisolve"
db_username          = "omnisolve"
db_password          = "CHANGE-TO-SECURE-PASSWORD"
db_instance_class    = "db.t3.micro"
db_allocated_storage = 20

# S3 bucket (must be globally unique)
s3_bucket_name       = "omnisolve-documents-prod-YOUR-ACCOUNT-ID"
```

**Important:** Add `terraform.tfvars` to `.gitignore` to avoid committing secrets.

---

## Local Development

### Quick Start with Docker Compose

```bash
# Start all services
docker compose up --build

# API available at:
# - http://localhost:8080
# - http://localhost:8080/swagger-ui/index.html
```

### Manual Setup

```bash
# Start PostgreSQL
docker run -d \
  --name omnisolve-postgres \
  -e POSTGRES_DB=omnisolve \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=admin \
  -p 5432:5432 \
  postgres:15

# Build application
./mvnw clean package

# Run application
./mvnw spring-boot:run
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific test
./mvnw test -Dtest=SmokeTest

# With coverage
./mvnw test jacoco:report
```

---

## Production Deployment

### Option 1: Automatic Deployment (Recommended)

1. **Commit and Push to Main**

```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

2. **Monitor GitHub Actions**
   - Go to `Actions` tab
   - Watch workflow progress
   - Check for any errors

3. **Verify Deployment**
   - Check Terraform outputs in workflow logs
   - Verify RDS instance in AWS Console
   - Verify S3 bucket creation

### Option 2: Manual Terraform Deployment

```bash
cd infrastructure/terraform

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Apply changes
terraform apply

# View outputs
terraform output
```

### Option 3: Manual Workflow Trigger

1. Go to `Actions` tab in GitHub
2. Select `Terraform Apply` workflow
3. Click `Run workflow`
4. Select `main` branch
5. Click `Run workflow` button

---

## Post-Deployment

### 1. Get Infrastructure Details

```bash
cd infrastructure/terraform

# Get all outputs
terraform output

# Get specific values
terraform output postgres_endpoint
terraform output postgres_jdbc_url
terraform output documents_bucket_name
```

### 2. Update Application Configuration

Create production configuration file or set environment variables:

```bash
export DB_URL="jdbc:postgresql://your-rds-endpoint:5432/omnisolve"
export DB_USERNAME="omnisolve"
export DB_PASSWORD="your-secure-password"
export DOCUMENT_BUCKET="your-s3-bucket-name"
export AWS_REGION="us-east-1"
export JWT_ENABLED="false"  # Enable after testing
```

### 3. Test Database Connection

```bash
# Using psql
psql -h your-rds-endpoint -U omnisolve -d omnisolve

# Or using Docker
docker run -it --rm postgres:15 \
  psql -h your-rds-endpoint -U omnisolve -d omnisolve
```

### 4. Verify API Endpoints

```bash
# Health check
curl https://your-api-domain/api/health

# List departments
curl https://your-api-domain/api/departments

# Swagger UI
open https://your-api-domain/swagger-ui/index.html
```

---

## Enabling Authentication

After testing, re-enable JWT authentication:

### 1. Restore Controller Annotations

Update all controllers to add back:
```java
@SecurityRequirement(name = "bearerAuth")
```

And restore JWT parameters:
```java
@AuthenticationPrincipal Jwt jwt
```

### 2. Configure AWS Cognito

```bash
# Create User Pool
aws cognito-idp create-user-pool \
  --pool-name omnisolve-users \
  --policies "PasswordPolicy={MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true}"

# Create User Pool Client
aws cognito-idp create-user-pool-client \
  --user-pool-id your-pool-id \
  --client-name omnisolve-api
```

### 3. Update Application Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/your-pool-id

app:
  security:
    jwt:
      enabled: true
    cognito:
      audience: your-client-id
```

---

## Monitoring

### CloudWatch Logs

```bash
# View RDS logs
aws logs tail /aws/rds/instance/omnisolve-dev-postgres/postgresql --follow

# View application logs (if using ECS/EKS)
aws logs tail /ecs/omnisolve-api --follow
```

### RDS Monitoring

```bash
# Check RDS status
aws rds describe-db-instances \
  --db-instance-identifier omnisolve-dev-postgres

# View metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name CPUUtilization \
  --dimensions Name=DBInstanceIdentifier,Value=omnisolve-dev-postgres \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Average
```

### S3 Monitoring

```bash
# List bucket contents
aws s3 ls s3://your-bucket-name/

# Check bucket size
aws s3 ls s3://your-bucket-name/ --recursive --summarize
```

---

## Troubleshooting

### RDS Connection Issues

**Problem:** Cannot connect to RDS

**Solutions:**
1. Check security group rules
```bash
aws ec2 describe-security-groups \
  --group-ids sg-xxxxx
```

2. Verify RDS is available
```bash
aws rds describe-db-instances \
  --db-instance-identifier omnisolve-dev-postgres \
  --query 'DBInstances[0].DBInstanceStatus'
```

3. Test from EC2 instance in same VPC
```bash
# Launch EC2 instance in same VPC
# SSH into instance
psql -h your-rds-endpoint -U omnisolve -d omnisolve
```

### Terraform State Lock

**Problem:** State is locked

**Solution:**
```bash
# View locks
aws dynamodb scan \
  --table-name omnisolve-terraform-lock \
  --region us-east-1

# Force unlock (use with caution)
terraform force-unlock LOCK_ID
```

### Application Won't Start

**Problem:** Application fails to start

**Checklist:**
- [ ] Database is accessible
- [ ] Environment variables are set correctly
- [ ] Flyway migrations completed successfully
- [ ] S3 bucket exists and is accessible
- [ ] AWS credentials are valid

**Check logs:**
```bash
# Docker logs
docker logs omnisolve-api

# Application logs
tail -f logs/spring.log
```

### S3 Access Denied

**Problem:** Cannot upload to S3

**Solutions:**
1. Check IAM permissions
2. Verify bucket policy
3. Check AWS credentials

```bash
# Test S3 access
aws s3 cp test.txt s3://your-bucket-name/test.txt
```

---

## Rollback Procedures

### Application Rollback

```bash
# Revert code
git revert HEAD
git push origin main

# Or deploy specific version
git checkout previous-commit
git push origin main --force
```

### Infrastructure Rollback

```bash
cd infrastructure/terraform

# Revert to previous state
git checkout previous-commit infrastructure/terraform/

# Apply previous configuration
terraform plan
terraform apply
```

### Database Rollback

```bash
# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier omnisolve-dev-postgres-restored \
  --db-snapshot-identifier snapshot-name
```

---

## Maintenance

### Database Backups

Automated backups are enabled with 7-day retention.

Manual snapshot:
```bash
aws rds create-db-snapshot \
  --db-instance-identifier omnisolve-dev-postgres \
  --db-snapshot-identifier omnisolve-manual-snapshot-$(date +%Y%m%d)
```

### Updating Dependencies

```bash
# Update Maven dependencies
./mvnw versions:display-dependency-updates

# Update specific dependency
./mvnw versions:use-latest-versions -Dincludes=org.springframework.boot:*
```

### Scaling

**Vertical Scaling (RDS):**
```hcl
# Update terraform.tfvars
db_instance_class = "db.t3.small"  # or larger

# Apply changes
terraform apply
```

**Horizontal Scaling:**
- Deploy multiple application instances
- Use load balancer (ALB/NLB)
- Configure connection pooling

---

## Security Checklist

- [ ] Secrets stored in GitHub Secrets
- [ ] Database password is strong and unique
- [ ] RDS in private subnets
- [ ] Security groups properly configured
- [ ] S3 bucket public access blocked
- [ ] Encryption enabled (RDS and S3)
- [ ] IAM least privilege principle applied
- [ ] Regular security updates applied
- [ ] Monitoring and alerting configured
- [ ] Backup strategy implemented

---

## Cost Optimization

Current monthly costs (estimated):
- RDS db.t3.micro: ~$15
- S3 storage: ~$0.023/GB
- Data transfer: Minimal (same region)
- **Total: $15-25/month**

Optimization tips:
- Use Reserved Instances for RDS (save up to 60%)
- Enable S3 Intelligent-Tiering
- Delete old snapshots
- Monitor and optimize queries
- Use CloudWatch alarms for cost anomalies

---

## Support

For issues or questions:
- Check [CI/CD Summary](CI_CD_SUMMARY.md)
- Review [Quick Start Guide](.github/QUICK_START.md)
- Check GitHub Actions logs
- Review AWS CloudWatch logs
- Contact DevOps team

---

## Additional Resources

- [AWS RDS Documentation](https://docs.aws.amazon.com/rds/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
