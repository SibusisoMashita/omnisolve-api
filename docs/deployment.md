# Deployment Guide

## Overview

OmniSolve API uses a fully automated CI/CD pipeline built with GitHub Actions. The pipeline builds, tests, and deploys the application to AWS Elastic Beanstalk environments.

## CI/CD Pipeline

### Pipeline Diagram

```mermaid
graph TB
    A[Push to main] --> B[Build Application]
    B --> C[Run Tests]
    C --> D[Code Quality - Qodana]
    D --> E[Build Docker Image]
    E --> F[Push to GHCR]
    F --> G[Deploy to DEV]
    G --> H{Health Check}
    H -->|Pass| I{Manual Approval}
    H -->|Fail| J[Notify Team]
    I -->|Approved| K[Deploy to PROD]
    K --> L{Health Check}
    L -->|Pass| M[Success]
    L -->|Fail| N[Rollback]
    
    style A fill:#9E9E9E
    style B fill:#4CAF50
    style C fill:#2196F3
    style D fill:#FF9800
    style E fill:#9C27B0
    style G fill:#00BCD4
    style K fill:#F44336
    style M fill:#4CAF50
```

### Pipeline Stages

#### 1. Build Application

**Trigger**: Push to `main` branch or pull request

**Actions**:
- Checkout code
- Set up JDK 21 (Temurin distribution)
- Build with Maven: `./mvnw -B package -DskipTests` (no clean for incremental compilation)
- Upload JAR artifact for downstream jobs
- Upload Maven build cache (compiled classes) for test stage

**Artifacts**: 
- `omnisolve-api-0.0.1.jar` (retained 30 days)
- Maven build cache (retained 1 day)

**Caching**:
- Maven dependencies cached via `actions/setup-java@v4`
- Compiled classes uploaded for test stage reuse

#### 2. Run Tests

**Dependencies**: Build stage

**Actions**:
- Spin up PostgreSQL 15 service container
- Download Maven build cache (compiled classes from build stage)
- Run tests: `./mvnw -B test` (incremental compilation, reuses compiled classes)
- Tests use embedded PostgreSQL or Testcontainers

**Environment Variables**:
```yaml
DB_URL: jdbc:postgresql://localhost:5432/omnisolve_test
DB_USERNAME: postgres
DB_PASSWORD: postgres
JWT_ENABLED: false
```

**Performance Optimization**:
- Reuses compiled classes from build stage
- Avoids full recompilation
- Maven dependency cache enabled

#### 3. Code Quality - Qodana

**Dependencies**: Test stage

**Actions**:
- Run JetBrains Qodana static analysis
- Post results as PR comments
- Upload results to Qodana Cloud

**Features**:
- Code quality checks
- Security vulnerability scanning
- Best practice violations
- Code smell detection

#### 4. Build Docker Image

**Dependencies**: Qodana stage

**Trigger**: Only on `main` branch push

**Actions**:
- Download JAR artifact
- Build multi-platform Docker image (linux/amd64, linux/arm64)
- Push to GitHub Container Registry (GHCR)

**Image Tags**:
- `main` - Latest main branch
- `sha-<commit>` - Specific commit
- `latest` - Latest release

**Caching**:
- Docker layer cache via GitHub Actions cache
- Scoped cache: `omnisolve-api` (avoids collisions)
- BuildKit cache mode: `max` (aggressive caching)

#### 5. Deploy to DEV

**Dependencies**: Qodana stage

**Trigger**: Only on `main` branch push

**Environment**: `development`

**Actions**:
1. Download JAR artifact
2. Configure AWS credentials
3. Generate version label: `dev-YYYYMMDD-HHMMSS-<commit>`
4. Deploy to Elastic Beanstalk DEV environment
5. Wait for deployment completion (max 5 minutes)
6. Verify health check: `GET /api/health`

**Deployment Package**: JAR file (not Docker image)

#### 6. Deploy to PROD

**Dependencies**: Deploy to DEV

**Trigger**: Manual approval required

**Environment**: `production`

**Actions**:
1. Download same JAR artifact used in DEV
2. Verify artifact integrity (checksum)
3. Generate version label: `prod-YYYYMMDD-HHMMSS-<commit>`
4. Deploy to Elastic Beanstalk PROD environment
5. Wait for deployment completion (max 5 minutes)
6. Verify health check: `GET /api/health`

---

## Deployment Environments

### Local Development

**Purpose**: Developer workstations

**Setup**:
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Run application
./mvnw spring-boot:run
```

**Configuration**:
- Profile: `local`
- Database: Docker Compose PostgreSQL
- S3: Local bucket or mock
- JWT: Disabled

### DEV Environment

**Purpose**: Integration testing and QA

**Infrastructure**:
- Elastic Beanstalk single-instance
- RDS PostgreSQL t3.micro
- S3 bucket: `dev-omnisolve-documents`

**Deployment**:
- Automatic on push to `main`
- No approval required

**Configuration**:
- Profile: `dev`
- JWT: Enabled
- Cognito: DEV user pool

### PROD Environment

**Purpose**: Production workloads

**Infrastructure**:
- Elastic Beanstalk single-instance
- RDS PostgreSQL t3.micro
- S3 bucket: `prod-omnisolve-documents`

**Deployment**:
- Manual approval required
- Same artifact as DEV (tested)

**Configuration**:
- Profile: `prod`
- JWT: Enabled
- Cognito: PROD user pool

---

## GitHub Secrets Configuration

### Required Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS IAM access key | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `EB_APPLICATION_NAME_DEV` | Elastic Beanstalk app name (DEV) | `dev-omnisolve-api` |
| `EB_ENVIRONMENT_NAME_DEV` | Elastic Beanstalk env name (DEV) | `dev-omnisolve-api` |
| `EB_APPLICATION_NAME_PROD` | Elastic Beanstalk app name (PROD) | `prod-omnisolve-api` |
| `EB_ENVIRONMENT_NAME_PROD` | Elastic Beanstalk env name (PROD) | `prod-omnisolve-api` |
| `QODANA_TOKEN` | Qodana Cloud token | `qdt_...` |

### Setting Secrets

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add each secret with its value

---

## Manual Deployment

### Prerequisites

- AWS CLI configured
- Elastic Beanstalk CLI installed
- JAR file built locally

### Deploy to DEV

```bash
# Build application
./mvnw clean package -DskipTests

# Deploy using script
./scripts/deploy-to-beanstalk.sh dev
```

### Deploy to PROD

```bash
# Build application
./mvnw clean package -DskipTests

# Deploy using script
./scripts/deploy-to-beanstalk.sh prod
```

### Using AWS CLI

```bash
# Create application version
aws elasticbeanstalk create-application-version \
  --application-name prod-omnisolve-api \
  --version-label manual-$(date +%Y%m%d-%H%M%S) \
  --source-bundle S3Bucket=omnisolve-deployments,S3Key=omnisolve-api-0.0.1.jar

# Update environment
aws elasticbeanstalk update-environment \
  --application-name prod-omnisolve-api \
  --environment-name prod-omnisolve-api \
  --version-label manual-$(date +%Y%m%d-%H%M%S)
```

---

## Rollback Procedures

### Automatic Rollback

Elastic Beanstalk automatically rolls back if:
- Deployment fails
- Health checks fail after deployment
- Environment becomes unhealthy

### Manual Rollback

#### Via AWS Console

1. Go to Elastic Beanstalk → Environments
2. Select the environment
3. Click "Application versions"
4. Select previous working version
5. Click "Deploy"

#### Via AWS CLI

```bash
# List recent versions
aws elasticbeanstalk describe-application-versions \
  --application-name prod-omnisolve-api \
  --max-records 10

# Deploy previous version
aws elasticbeanstalk update-environment \
  --application-name prod-omnisolve-api \
  --environment-name prod-omnisolve-api \
  --version-label prod-20240115-143000-abc1234
```

#### Via GitHub Actions

1. Go to Actions → Main CI/CD Pipeline
2. Find the previous successful deployment
3. Click "Re-run jobs" → "Re-run failed jobs"

---

## Environment Variables

### Elastic Beanstalk Configuration

Environment variables are set in Terraform or via AWS Console:

```hcl
setting {
  namespace = "aws:elasticbeanstalk:application:environment"
  name      = "SPRING_PROFILES_ACTIVE"
  value     = "prod"
}

setting {
  namespace = "aws:elasticbeanstalk:application:environment"
  name      = "SPRING_DATASOURCE_URL"
  value     = "jdbc:postgresql://..."
}
```

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev`, `prod` |
| `SPRING_DATASOURCE_URL` | Database JDBC URL | `jdbc:postgresql://...` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `omnisolve` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `<secret>` |
| `AWS_REGION` | AWS region | `us-east-1` |
| `AWS_S3_BUCKET` | S3 bucket name | `prod-omnisolve-documents` |
| `JWT_ENABLED` | Enable JWT auth | `true`, `false` |
| `COGNITO_ISSUER_URI` | Cognito issuer URI | `https://cognito-idp...` |
| `COGNITO_AUDIENCE` | Cognito client ID | `<client-id>` |

---

## Deployment Checklist

### Pre-Deployment

- [ ] All tests passing locally
- [ ] Code reviewed and approved
- [ ] Database migrations tested
- [ ] Environment variables configured
- [ ] Secrets rotated if needed
- [ ] Backup database (PROD only)

### During Deployment

- [ ] Monitor GitHub Actions pipeline
- [ ] Watch Elastic Beanstalk events
- [ ] Check CloudWatch logs
- [ ] Verify health check endpoint

### Post-Deployment

- [ ] Smoke test critical endpoints
- [ ] Verify database migrations applied
- [ ] Check S3 file uploads
- [ ] Monitor error rates
- [ ] Update deployment documentation

---

## Monitoring Deployment

### GitHub Actions

View pipeline progress:
```
https://github.com/<org>/<repo>/actions
```

### Elastic Beanstalk Events

```bash
# Watch environment events
aws elasticbeanstalk describe-events \
  --environment-name prod-omnisolve-api \
  --max-records 20
```

### CloudWatch Logs

```bash
# Tail application logs
aws logs tail /aws/elasticbeanstalk/prod-omnisolve-api/var/log/application.log \
  --follow
```

### Health Check

```bash
# Check application health
curl http://<environment-url>/api/health

# Expected response
{"status":"ok"}
```

---

## Troubleshooting Deployments

### Deployment Fails

**Symptoms**: Pipeline fails at deploy stage

**Solutions**:
1. Check GitHub Actions logs
2. Verify AWS credentials are valid
3. Check Elastic Beanstalk events
4. Verify artifact was uploaded correctly

### Health Check Fails

**Symptoms**: Deployment completes but health check returns non-200

**Solutions**:
1. Check application logs in CloudWatch
2. Verify database connection
3. Check environment variables
4. Verify S3 bucket permissions

### Database Connection Issues

**Symptoms**: Application starts but can't connect to database

**Solutions**:
1. Verify RDS security group allows Beanstalk access
2. Check database credentials
3. Verify database is running
4. Check VPC/subnet configuration

### S3 Access Denied

**Symptoms**: File uploads fail with 403 errors

**Solutions**:
1. Verify IAM instance profile has S3 permissions
2. Check S3 bucket policy
3. Verify bucket name is correct
4. Check AWS region configuration

---

## Pipeline Performance and Caching

### Caching Strategy

The pipeline implements multiple caching layers for optimal performance:

#### 1. Maven Dependency Cache

**Implementation**:
```yaml
- uses: actions/setup-java@v4
  with:
    cache: maven
```

**Caches**: `~/.m2/repository`

**Benefits**:
- Maven dependencies reused between runs
- Dependency downloads skipped if unchanged
- Reduces build time by 2-4 minutes

#### 2. Maven Build Cache

**Implementation**:
- Build stage uploads compiled classes
- Test stage downloads and reuses them

**Caches**:
- `target/classes`
- `target/generated-sources`
- `target/maven-status`

**Benefits**:
- Avoids full recompilation in test stage
- Incremental compilation only
- Reduces test stage time by 40-60 seconds

#### 3. Docker Layer Cache

**Implementation**:
```yaml
cache-from: type=gha,scope=omnisolve-api
cache-to: type=gha,mode=max,scope=omnisolve-api
```

**Benefits**:
- Docker layers cached across workflow runs
- Subsequent builds much faster
- Scoped cache avoids collisions
- Reduces Docker build time by 2-3 minutes

#### 4. Qodana Analysis Cache

**Implementation**:
```yaml
use-caches: true
```

**Benefits**:
- Analysis results cached
- Faster subsequent scans
- Reduces Qodana time by 30-50%

### Build Time Comparison

| Stage | First Run | Cached Run | Improvement |
|-------|-----------|------------|-------------|
| Maven dependency download | 2-4 min | 0 sec | ~3 min |
| Build compile | 60-90 sec | 60-90 sec | 0 sec |
| Test compile | 60-90 sec | 20-30 sec | ~50 sec |
| Docker build | 2-3 min | 20-40 sec | ~2 min |
| Qodana scan | 3-5 min | 2-3 min | ~2 min |
| **Total Pipeline** | **10-12 min** | **4-6 min** | **~6 min (50%)** |

### Optimization Techniques

#### Incremental Compilation

**Before**:
```bash
# Build stage
./mvnw clean package -DskipTests  # Deletes compiled classes

# Test stage
./mvnw test  # Recompiles everything
```

**After**:
```bash
# Build stage
./mvnw -B package -DskipTests  # Keeps compiled classes

# Test stage
./mvnw -B test  # Reuses compiled classes, incremental only
```

**Result**: Test stage 40-60% faster

#### Docker Layer Optimization

**Dockerfile Structure**:
```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy JAR last (changes most frequently)
COPY target/omnisolve-api-0.0.1.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Benefits**:
- Base image layer cached
- Only JAR layer rebuilt on changes
- Faster subsequent builds

#### Artifact Reuse

**Strategy**:
- Build JAR once in build stage
- Reuse same artifact in:
  - Docker build
  - DEV deployment
  - PROD deployment

**Benefits**:
- Consistent artifact across environments
- No rebuild overhead
- Faster deployments

### Cache Invalidation

Caches are automatically invalidated when:

- **Maven dependencies**: `pom.xml` changes
- **Maven build**: Source code changes
- **Docker layers**: `Dockerfile` or JAR changes
- **Qodana**: Code changes

### Best Practices

1. **Avoid `clean` in CI**: Let Maven use incremental compilation
2. **Use `-B` flag**: Batch mode for CI (no interactive prompts)
3. **Scope Docker cache**: Prevent collisions with other projects
4. **Upload minimal artifacts**: Only what's needed downstream
5. **Set retention policies**: Balance storage vs. reusability

---

## Performance Optimization

### Build Time

- Maven dependency caching enabled
- Docker layer caching enabled
- Parallel test execution
- Incremental compilation

### Deployment Time

- Single-instance deployment: ~3-5 minutes
- Load-balanced deployment: ~10-15 minutes

### Artifact Size

- JAR file: ~50-80 MB
- Docker image: ~200-300 MB

---

## Security Considerations

### Secrets Management

- Never commit secrets to Git
- Use GitHub Secrets for CI/CD
- Rotate credentials regularly
- Use AWS Secrets Manager for production

### IAM Permissions

- Principle of least privilege
- Separate IAM users for CI/CD
- Use IAM roles for EC2 instances
- Enable MFA for production access

### Network Security

- Private subnets for RDS
- Security groups restrict access
- HTTPS only in production
- VPC isolation

---

## Disaster Recovery

### Backup Strategy

- **Database**: Automated RDS backups (7-day retention)
- **S3**: Versioning enabled on document bucket
- **Code**: Git repository (GitHub)
- **Infrastructure**: Terraform state in S3

### Recovery Procedures

1. **Database Restore**:
   ```bash
   aws rds restore-db-instance-from-db-snapshot \
     --db-instance-identifier prod-omnisolve-postgres-restored \
     --db-snapshot-identifier <snapshot-id>
   ```

2. **Application Redeploy**:
   - Re-run GitHub Actions pipeline
   - Or deploy previous version manually

3. **Infrastructure Rebuild**:
   ```bash
   cd infrastructure/prod
   terraform apply
   ```
