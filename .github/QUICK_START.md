# Quick Start Guide

Get your OmniSolve API up and running in minutes.

## Prerequisites

- Git
- Java 21+
- Docker & Docker Compose
- AWS Account (for production deployment)
- GitHub Account

## Local Development

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/omnisolve-api.git
cd omnisolve-api
```

### 2. Start Local Environment

```bash
# Start PostgreSQL + API with Docker Compose
docker compose up --build
```

The API will be available at:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Health Check**: http://localhost:8080/api/health

### 3. Test the API

```bash
# Health check
curl http://localhost:8080/api/health

# List departments
curl http://localhost:8080/api/departments

# Create a department
curl -X POST http://localhost:8080/api/departments \
  -H "Content-Type: application/json" \
  -d '{"name": "Engineering", "description": "Engineering Department"}'
```

## Production Deployment

### 1. Initial Setup

Follow the [Initial Setup Checklist](.github/INITIAL_SETUP_CHECKLIST.md) to configure:
- GitHub repository
- AWS credentials
- GitHub Secrets
- Terraform variables

### 2. Deploy Infrastructure

Push to `main` branch or manually trigger the workflow:

```bash
git add .
git commit -m "Initial commit"
git push origin main
```

GitHub Actions will automatically:
1. Run tests
2. Build Docker image
3. Deploy AWS infrastructure (RDS + S3)

### 3. Monitor Deployment

- Go to GitHub Actions tab
- Watch the workflow progress
- Check deployment summary for connection details

### 4. Access Production API

After deployment, get the RDS endpoint from Terraform outputs:

```bash
cd infrastructure/terraform
terraform output postgres_jdbc_url
```

Update your application configuration with the production database URL.

## Development Workflow

### Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

### Building Locally

```bash
# Build without tests
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package
```

### Running Without Docker

```bash
# Start PostgreSQL separately
docker run -d \
  -e POSTGRES_DB=omnisolve \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=admin \
  -p 5432:5432 \
  postgres:15

# Run Spring Boot
./mvnw spring-boot:run
```

## Common Tasks

### View Logs

```bash
# Docker Compose logs
docker compose logs -f

# Specific service
docker compose logs -f api
```

### Database Access

```bash
# Connect to local PostgreSQL
docker compose exec postgres psql -U postgres -d omnisolve

# Or using psql directly
psql -h localhost -U postgres -d omnisolve
```

### Rebuild After Changes

```bash
# Rebuild and restart
docker compose up --build

# Or rebuild specific service
docker compose up --build api
```

### Clean Up

```bash
# Stop services
docker compose down

# Remove volumes (deletes data)
docker compose down -v
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in compose.yaml
```

### Database Connection Failed

- Ensure PostgreSQL container is running: `docker compose ps`
- Check logs: `docker compose logs postgres`
- Verify connection string in `application.yml`

### Build Failures

```bash
# Clean Maven cache
./mvnw clean

# Update dependencies
./mvnw dependency:resolve

# Rebuild
./mvnw clean install
```

## Next Steps

- [ ] Explore API documentation at `/swagger-ui/index.html`
- [ ] Review [Deployment Guide](../DEPLOYMENT.md)
- [ ] Set up monitoring and alerts
- [ ] Configure CI/CD pipeline
- [ ] Enable authentication (see Initial Setup Checklist)

## Resources

- [README](../README.md) - Full project documentation
- [Deployment Guide](../DEPLOYMENT.md) - Detailed deployment instructions
- [CI/CD Summary](../CI_CD_SUMMARY.md) - Pipeline overview
- [Terraform Documentation](https://www.terraform.io/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
