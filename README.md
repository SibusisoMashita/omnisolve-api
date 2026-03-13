# OmniSolve API

A comprehensive multi-tenant SaaS platform for compliance management, providing document control, incident management, contractor management, and asset inspection capabilities for organizations managing ISO 9001, ISO 14001, and ISO 45001 compliance.

Developed by [Valo Systems](https://valosystems.co.za/)

## Tech Stack

- **Backend**: Spring Boot 3.3.5, Java 21
- **Database**: PostgreSQL 16 with Flyway migrations
- **Security**: Spring Security, OAuth2 JWT (AWS Cognito)
- **Cloud**: AWS (Elastic Beanstalk, RDS, S3)
- **Infrastructure**: Terraform
- **CI/CD**: GitHub Actions
- **Containerization**: Docker
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: JUnit, Embedded PostgreSQL

## Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose (for local development)

### Run Locally

```bash
# Start PostgreSQL with Docker Compose
docker-compose up -d postgres

# Optional: disable JWT auth for local development
export JWT_ENABLED=false

# Run the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:5000`

Swagger UI: `http://localhost:5000/swagger-ui.html`

## Core Features

- **Document Control**: Manage policies, procedures, and manuals with workflow, versioning, and ISO clause linking
- **Incident Management**: Track workplace incidents from report through investigation to closure
- **Contractor Management**: Manage contractor compliance, workers, and document requirements
- **Asset Inspections**: Conduct inspections on assets with checklists, findings, and attachments
- **Multi-Tenancy**: Complete data isolation between organizations with shared infrastructure
- **RBAC**: Role-based access control with organization-scoped permissions
- **Audit Trail**: Comprehensive audit logging for compliance tracking
- **AWS Integration**: S3 storage, Cognito authentication, and Elastic Beanstalk hosting

## Repository Structure

```
omnisolve-api/
├── src/                    # Application source code
│   ├── main/java/          # Java source files
│   │   └── com/omnisolve/  # Main package
│   │       ├── assurance/  # Asset inspection module
│   │       ├── contractor/ # Contractor management module
│   │       ├── audit/      # Audit logging infrastructure
│   │       ├── config/     # Configuration classes
│   │       ├── controller/ # REST controllers (core modules)
│   │       ├── domain/     # JPA entities (core modules)
│   │       ├── event/      # Domain events
│   │       ├── repository/ # Data repositories
│   │       ├── security/   # Security configuration
│   │       ├── service/    # Business logic
│   │       └── tenant/     # Multi-tenancy context
│   └── main/resources/     # Application properties & DB migrations
├── infrastructure/         # Terraform IaC
│   ├── modules/omnisolve/  # Reusable Terraform module
│   ├── dev/                # Development environment
│   └── prod/               # Production environment
├── .github/workflows/      # CI/CD pipeline definitions
├── scripts/                # Deployment and utility scripts
├── docs/                   # Comprehensive documentation
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

**Getting Started:**
- [System Overview](./docs/system-overview.md) - Complete system introduction and capabilities

**Architecture & Design:**
- [Architecture](./docs/architecture.md) - System architecture and design patterns
- [Backend Structure](./docs/backend-structure.md) - Package organization and conventions
- [Database](./docs/database.md) - Schema design and migration strategy
- [Multi-Tenancy](./docs/multi-tenancy.md) - Tenant isolation and data scoping
- [Security](./docs/security.md) - Authentication, authorization, and JWT validation

**Modules:**
- [Document Control](./docs/document-module.md) - Document lifecycle and version management
- [Incident Management](./docs/incident-module.md) - Incident tracking and investigation
- [Contractor Management](./docs/contractor-module.md) - Contractor compliance and workers
- [Asset Inspections](./docs/assurance-module.md) - Asset inspections and checklists

**Development & Operations:**
- [Development Guide](./docs/development.md) - Local setup and testing
- [API Endpoints](./docs/api-endpoints.md) - Complete API reference
- [AWS Infrastructure](./docs/aws-infrastructure.md) - Cloud architecture and deployment

## License

Copyright © 2024 [Valo Systems](https://valosystems.co.za/). All rights reserved.
