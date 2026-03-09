# CI/CD Pipeline Summary

This document provides an overview of the automated CI/CD pipelines configured for the OmniSolve API project.

## Workflows Overview

### 1. CI Workflow (`ci.yml`)

**Triggers:**
- Pull requests to `main`
- Pushes to `main`

**Jobs:**
- **Build and Test**
  - Sets up Java 21 and PostgreSQL 15
  - Builds the application with Maven
  - Runs all tests
  - Uploads JAR artifact (on main branch only)
  
- **Code Quality**
  - Checks code formatting with Spotless
  - Runs static analysis

**Purpose:** Ensures code quality and functionality before merging.

---

### 2. Docker Build Workflow (`docker-build.yml`)

**Triggers:**
- Pushes to `main`
- Version tags (`v*`)

**Jobs:**
- **Build and Push Docker Image**
  - Builds multi-platform Docker image (amd64, arm64)
  - Pushes to GitHub Container Registry (ghcr.io)
  - Tags: branch name, SHA, semver, latest

**Purpose:** Creates containerized application ready for deployment.

---

### 3. Terraform Plan Workflow (`terraform-plan.yml`)

**Triggers:**
- Pull requests to `main` (when Terraform files change)

**Jobs:**
- **Terraform Plan**
  - Validates Terraform configuration
  - Generates execution plan
  - Posts plan as PR comment
  - Checks formatting

**Purpose:** Preview infrastructure changes before applying.

---

### 4. Terraform Apply Workflow (`terraform-apply.yml`)

**Triggers:**
- Pushes to `main` (when Terraform files change)

**Jobs:**
- **Terraform Apply**
  - Initializes Terraform
  - Applies infrastructure changes
  - Outputs infrastructure details
  - Uploads outputs as artifact

**Purpose:** Automatically deploys infrastructure changes to AWS.

---

### 5. Qodana Code Quality (`qodana_code_quality.yml`)

**Triggers:**
- Pull requests
- Pushes to main

**Jobs:**
- Runs Qodana static analysis
- Generates code quality reports

**Purpose:** Advanced code quality and security analysis.

---

## Workflow Execution Flow

### For Pull Requests

```
PR Created/Updated
    ↓
CI Workflow (Build & Test)
    ↓
Terraform Plan (if infra changed)
    ↓
Qodana Code Quality
    ↓
Review & Approve
```

### For Main Branch Pushes

```
Push to Main
    ↓
CI Workflow (Build & Test)
    ↓
Docker Build & Push
    ↓
Terraform Apply (if infra changed)
    ↓
Infrastructure Deployed
```

---

## Required GitHub Secrets

Configure these in: `Settings` → `Secrets and variables` → `Actions`

### AWS Credentials

| Secret Name | Description | Used By |
|-------------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS IAM access key | Terraform workflows |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret key | Terraform workflows |

### Terraform Variables

| Secret Name | Description | Used By |
|-------------|-------------|---------|
| `TF_VAR_VPC_ID` | AWS VPC ID | Terraform workflows |
| `TF_VAR_PRIVATE_SUBNET_IDS` | Subnet IDs (JSON array) | Terraform workflows |
| `TF_VAR_ALLOWED_CIDR_BLOCKS` | CIDR blocks (JSON array) | Terraform workflows |
| `TF_VAR_DB_PASSWORD` | Database password | Terraform workflows |
| `TF_VAR_S3_BUCKET_NAME` | S3 bucket name | Terraform workflows |

### Docker Registry (Optional)

| Secret Name | Description | Used By |
|-------------|-------------|---------|
| `DOCKER_USERNAME` | Docker Hub username | Docker build (if using Docker Hub) |
| `DOCKER_PASSWORD` | Docker Hub password | Docker build (if using Docker Hub) |
| `GITHUB_TOKEN` | Auto-generated | Docker build, PR comments |

**Note:** The current setup uses GitHub Container Registry (ghcr.io) which uses `GITHUB_TOKEN` automatically.

---

## Infrastructure Components

The Terraform workflows manage:

- **RDS PostgreSQL 15** (db.t3.micro)
  - Single-AZ for cost optimization
  - Encrypted storage
  - 7-day backup retention
  
- **S3 Bucket**
  - Document storage
  - Versioning enabled
  - Server-side encryption (AES256)
  - Public access blocked
  
- **Security Groups**
  - PostgreSQL access control
  - VPC-restricted access
  
- **Subnet Groups**
  - Multi-AZ subnet configuration

---

## Deployment Strategy

### Development
- Local development with Docker Compose
- No authentication required (JWT disabled)
- Uses local PostgreSQL container

### Production
- Automated deployment on merge to `main`
- Infrastructure managed by Terraform
- Docker images pushed to GitHub Container Registry
- JWT authentication enabled (after testing)

---

## Monitoring Deployment

### GitHub Actions UI
1. Go to `Actions` tab in repository
2. Select workflow run
3. View logs and status

### Terraform Outputs
After successful deployment, check artifacts:
- Download `infrastructure-outputs` artifact
- Contains RDS endpoint, S3 bucket name, etc.

### AWS Console
- RDS: Check database status
- S3: Verify bucket creation
- CloudWatch: Monitor logs and metrics

---

## Rollback Procedures

### Application Rollback
```bash
# Revert to previous Docker image
docker pull ghcr.io/your-org/omnisolve-api:previous-sha
```

### Infrastructure Rollback
```bash
cd infrastructure/terraform
git checkout previous-commit
terraform plan
terraform apply
```

---

## Cost Optimization

Current configuration is optimized for low cost:
- **RDS**: db.t3.micro (~$15/month)
- **S3**: Pay per use (~$0.023/GB)
- **Data Transfer**: Minimal in same region
- **No Multi-AZ**: Saves ~50% on RDS cost

**Estimated Monthly Cost:** $15-25 USD

---

## Security Best Practices

✅ Secrets stored in GitHub Secrets (encrypted)
✅ IAM user with minimal required permissions
✅ RDS in private subnets
✅ S3 public access blocked
✅ Encrypted storage (RDS and S3)
✅ Security groups restrict access
✅ Terraform state in encrypted S3 bucket
✅ State locking with DynamoDB

---

## Troubleshooting

### Workflow Failures

**Build Failures:**
- Check Java version compatibility
- Verify Maven dependencies
- Review test logs

**Terraform Failures:**
- Check AWS credentials
- Verify VPC and subnet configuration
- Review Terraform state lock
- Check AWS service quotas

**Docker Build Failures:**
- Verify Dockerfile syntax
- Check base image availability
- Review build logs

### Common Issues

**State Lock Error:**
```bash
# Force unlock (use with caution)
terraform force-unlock LOCK_ID
```

**RDS Creation Timeout:**
- RDS creation takes 5-10 minutes
- Check AWS console for status
- Verify subnet group has 2+ AZs

**Docker Push Permission Denied:**
- Verify GITHUB_TOKEN permissions
- Check package write permissions

---

## Next Steps

1. ✅ Authentication removed from controllers (for testing)
2. ⏳ Test API endpoints
3. ⏳ Re-enable JWT authentication
4. ⏳ Configure AWS Cognito
5. ⏳ Set up monitoring and alerts
6. ⏳ Configure custom domain
7. ⏳ Implement backup strategy

---

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Deployment](https://spring.io/guides/gs/spring-boot-docker/)
