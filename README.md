# OmniSolve API

A Spring Boot-based document control system for managing organizational documents, clauses, departments, and document types with version control and AWS S3 storage integration.

Developed by [Valo Systems](https://valosystems.co.za/)

## Tech Stack

- **Backend**: Spring Boot 3.3.5, Java 21
- **Database**: PostgreSQL 15 with Flyway migrations
- **Security**: Spring Security, OAuth2 JWT (AWS Cognito)
- **Cloud**: AWS (Elastic Beanstalk, RDS, S3)
- **Infrastructure**: Terraform
- **CI/CD**: GitHub Actions
- **Containerization**: Docker
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: JUnit, Testcontainers, Embedded PostgreSQL

## Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose (for local development)

### Run Locally

```bash
# Start PostgreSQL with Docker Compose
docker-compose up -d postgres

# Run the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Repository Structure

```
omnisolve-api/
├── src/                    # Application source code
│   ├── main/java/          # Java source files
│   │   └── com/omnisolve/  # Main package
│   │       ├── config/     # Configuration classes
│   │       ├── controller/ # REST controllers
│   │       ├── domain/     # JPA entities
│   │       ├── repository/ # Data repositories
│   │       ├── security/   # Security configuration
│   │       └── service/    # Business logic
│   └── main/resources/     # Application properties & DB migrations
├── infrastructure/         # Terraform IaC
│   ├── modules/omnisolve/  # Reusable Terraform module
│   ├── dev/                # Development environment
│   └── prod/               # Production environment
├── .github/workflows/      # CI/CD pipeline definitions
├── scripts/                # Deployment and utility scripts
├── Dockerfile              # Container image definition
└── docker-compose.yml      # Local development setup
```

## Deployment

The project uses a multi-stage GitHub Actions CI/CD pipeline that automatically:

1. Builds the application with Maven
2. Runs tests with PostgreSQL
3. Performs code quality checks (Qodana)
4. Builds and pushes Docker images to GHCR
5. Deploys to AWS Elastic Beanstalk DEV environment
6. Deploys to PROD environment (with manual approval)

Infrastructure is provisioned using Terraform with separate configurations for dev and prod environments.

## Documentation

Detailed documentation is available in the [`docs/`](./docs) directory:

- [Architecture](./docs/architecture.md) - System architecture and design
- [API Documentation](./docs/api.md) - API endpoints and usage
- [Deployment Guide](./docs/deployment.md) - CI/CD pipeline and deployment process
- [Infrastructure](./docs/infrastructure.md) - Terraform and AWS resources
- [Local Development](./docs/local-development.md) - Development environment setup
- [Troubleshooting](./docs/troubleshooting.md) - Common issues and solutions

## License

Copyright © 2024 [Valo Systems](https://valosystems.co.za/). All rights reserved.
