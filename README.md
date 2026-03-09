<div align="center">

# 🚀 OmniSolve API

**Enterprise Document Control System**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![AWS S3](https://img.shields.io/badge/AWS-S3-orange?logo=amazon-aws&logoColor=white)](https://aws.amazon.com/s3/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Terraform](https://img.shields.io/badge/Terraform-IaC-7B42BC?logo=terraform&logoColor=white)](https://www.terraform.io/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-6BA539?logo=swagger&logoColor=white)](https://www.openapis.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

*A robust backend service for managing controlled documents with workflow automation, version control, and AWS S3 integration.*

[Features](#-features) • [Quick Start](#-quick-start) • [API Documentation](#-api-documentation) • [Deployment](#-deployment)

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Project Structure](#-project-structure)
- [Configuration](#-configuration)
- [Database](#-database)
- [Deployment](#-deployment)
- [Security](#-security)
- [Development](#-development)

---

## ✨ Features

- 📄 **Document Management** - Full CRUD operations for controlled documents
- 🔄 **Workflow Engine** - Submit, approve, reject, and archive documents
- 📦 **Version Control** - S3-backed document versioning with file upload
- 🏢 **Department Management** - Organize documents by department ownership
- 📑 **ISO Clause Tracking** - Link documents to ISO compliance clauses
- 🔐 **JWT Authentication** - AWS Cognito integration for secure access
- 📊 **OpenAPI/Swagger** - Interactive API documentation
- 🗄️ **Database Migrations** - Flyway-managed schema versioning
- ☁️ **Cloud Ready** - Terraform infrastructure as code for AWS
- 🐳 **Docker Support** - One-command local development environment

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.5 (Web, Data JPA, Security) |
| **Database** | PostgreSQL with Flyway migrations |
| **Cloud Storage** | AWS S3 (SDK v2) |
| **Authentication** | OAuth2 Resource Server (AWS Cognito JWT) |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger UI |
| **Infrastructure** | Terraform (RDS PostgreSQL + S3) |
| **Containerization** | Docker & Docker Compose |
| **Build Tool** | Maven |

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.8+ (or use included wrapper)

### One-Command Startup

Run the complete local stack (PostgreSQL + API):

```bash
docker compose up --build
```

The API will be available at:
- **API Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

### Stop Services

```bash
docker compose down
```

### Manual Build & Run

```bash
# Build the project
./mvnw clean package

# Run with default profile
./mvnw spring-boot:run
```

---

## 📚 API Documentation

### Interactive Documentation

Once running, access the Swagger UI for interactive API exploration:

🔗 **http://localhost:8080/swagger-ui/index.html**

### API Endpoints

#### 🏥 Health Check

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/health` | Health check | ❌ No |

#### 📄 Documents

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/documents` | List all documents | ✅ Yes |
| `GET` | `/api/documents/{id}` | Get document by ID | ✅ Yes |
| `POST` | `/api/documents` | Create new document | ✅ Yes |
| `PUT` | `/api/documents/{id}` | Update document | ✅ Yes |
| `POST` | `/api/documents/{id}/submit` | Submit for approval | ✅ Yes |
| `POST` | `/api/documents/{id}/approve` | Approve document | ✅ Yes |
| `POST` | `/api/documents/{id}/reject` | Reject document | ✅ Yes |
| `POST` | `/api/documents/{id}/archive` | Archive document | ✅ Yes |
| `POST` | `/api/documents/{id}/versions` | Upload new version | ✅ Yes |

#### 🏢 Departments

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/departments` | List departments | ✅ Yes |
| `POST` | `/api/departments` | Create department | ✅ Yes |
| `PUT` | `/api/departments/{id}` | Update department | ✅ Yes |
| `DELETE` | `/api/departments/{id}` | Delete department | ✅ Yes |

#### 📑 Document Types

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/document-types` | List document types | ✅ Yes |
| `POST` | `/api/document-types` | Create document type | ✅ Yes |
| `PUT` | `/api/document-types/{id}` | Update document type | ✅ Yes |
| `DELETE` | `/api/document-types/{id}` | Delete document type | ✅ Yes |

#### 📋 Clauses

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/clauses` | List ISO clauses | ✅ Yes |
| `POST` | `/api/clauses` | Create clause | ✅ Yes |
| `PUT` | `/api/clauses/{id}` | Update clause | ✅ Yes |

### Example Requests

#### Create a Document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Quality Management Procedure",
    "documentNumber": "QMP-001",
    "departmentId": 1,
    "documentTypeId": 1,
    "clauseIds": [1, 2]
  }'
```

#### Upload Document Version

```bash
curl -X POST http://localhost:8080/api/documents/{id}/versions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@document.pdf"
```

---

## 📁 Project Structure

```
omnisolve-api/
├── src/main/java/com/omnisolve/
│   ├── controller/          # REST API endpoints
│   │   ├── DocumentController.java
│   │   ├── ClauseController.java
│   │   ├── DepartmentController.java
│   │   ├── DocumentTypeController.java
│   │   └── HealthController.java
│   ├── service/             # Business logic
│   │   ├── dto/             # Data transfer objects
│   │   ├── DocumentService.java
│   │   ├── ClauseService.java
│   │   ├── DepartmentService.java
│   │   ├── DocumentTypeService.java
│   │   └── AwsS3StorageService.java
│   ├── repository/          # JPA repositories
│   │   ├── DocumentRepository.java
│   │   ├── ClauseRepository.java
│   │   └── ...
│   ├── domain/              # JPA entities
│   │   ├── Document.java
│   │   ├── DocumentVersion.java
│   │   ├── Department.java
│   │   └── ...
│   ├── security/            # Security configuration
│   │   ├── JwtSecurityConfig.java
│   │   └── AudienceValidator.java
│   └── config/              # Application configuration
│       ├── OpenApiConfig.java
│       └── S3Config.java
├── src/main/resources/
│   ├── application.yml      # Application configuration
│   └── db/migration/        # Flyway SQL migrations
│       ├── V1__init.sql
│       ├── V2__seed_data.sql
│       └── V3__seed_documents.sql
├── infrastructure/terraform/ # AWS infrastructure
│   ├── main.tf
│   ├── postgres.tf
│   ├── s3.tf
│   └── variables.tf
├── compose.yaml             # Docker Compose configuration
├── Dockerfile               # Container image definition
└── pom.xml                  # Maven dependencies
```

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/omnisolve` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `admin` |
| `SERVER_PORT` | API server port | `8080` |
| `DOCUMENT_BUCKET` | S3 bucket name | `omnisolve-documents-dev` |
| `AWS_REGION` | AWS region | `us-east-1` |
| `COGNITO_ISSUER_URI` | Cognito issuer URI | Required for JWT |
| `COGNITO_AUDIENCE` | Cognito client ID | Required for JWT |
| `JWT_ENABLED` | Enable JWT authentication | `false` |

### Application Configuration

Edit `src/main/resources/application.yml` to customize:

- Database connection settings
- S3 bucket configuration
- JWT/Cognito settings
- JPA/Hibernate properties
- Flyway migration settings

---

## 🗄️ Database

### Flyway Migrations

Database schema is managed through Flyway migrations located in `src/main/resources/db/migration/`:

- **V1__init.sql** - Initial schema (tables, constraints, indexes)
- **V2__seed_data.sql** - Reference data (departments, document types, clauses)
- **V3__seed_documents.sql** - Sample documents for testing

Migrations run automatically on application startup.

### Local PostgreSQL

Docker Compose provides a local PostgreSQL instance:

- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `omnisolve`
- **Username**: `postgres`
- **Password**: `admin`

---

## ☁️ Deployment

### Quick Deploy

```bash
# 1. Setup (first time only)
# See .github/SETUP.md for detailed instructions

# 2. Deploy infrastructure
cd infrastructure/terraform
terraform init
terraform apply

# 3. Push to GitHub (automatic deployment)
git push origin main
```

### CI/CD Pipeline

The project includes automated GitHub Actions workflows:

- **Pull Requests**: Terraform plan, build, and test
- **Main Branch**: Deploy infrastructure, build Docker image, push to registry

See [Deployment Guide](DEPLOYMENT.md) for complete instructions.

### Manual Deployment

#### Terraform Infrastructure

```bash
cd infrastructure/terraform

# Copy and configure variables
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your settings

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Deploy infrastructure
terraform apply
```

#### Docker Deployment

```bash
# Build image
docker build -t omnisolve-api:latest .

# Run container
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://your-rds-endpoint:5432/omnisolve \
  -e DB_USERNAME=your-username \
  -e DB_PASSWORD=your-password \
  -e DOCUMENT_BUCKET=your-s3-bucket \
  -e JWT_ENABLED=true \
  -e COGNITO_ISSUER_URI=your-cognito-issuer \
  -e COGNITO_AUDIENCE=your-client-id \
  omnisolve-api:latest
```

### Infrastructure Outputs

After deployment, Terraform provides:

- PostgreSQL endpoint and JDBC URL
- S3 bucket name
- Security group IDs
- Connection details

### Documentation

- 📖 [Full Deployment Guide](DEPLOYMENT.md)
- 🚀 [Quick Start Guide](.github/QUICK_START.md)
- ⚙️ [GitHub Setup Guide](.github/SETUP.md)

---

## 🔐 Security

### Authentication

The API uses JWT-based authentication via AWS Cognito:

- **Protected Endpoints**: All `/api/*` endpoints except `/api/health`
- **Token Type**: Bearer JWT
- **Issuer**: AWS Cognito User Pool
- **Validation**: Audience claim validation

### Local Development

For local development, JWT authentication is disabled by default (`JWT_ENABLED=false`). Enable it by setting:

```yaml
app:
  security:
    jwt:
      enabled: true
```

### Authorization Header

Include JWT token in requests:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 💻 Development

### Build Commands

```bash
# Clean and build
./mvnw clean package

# Run tests
./mvnw test

# Run application
./mvnw spring-boot:run

# Skip tests during build
./mvnw clean package -DskipTests
```

### IDE Setup

Import as a Maven project in your favorite IDE:

- **IntelliJ IDEA**: File → Open → Select `pom.xml`
- **Eclipse**: File → Import → Existing Maven Projects
- **VS Code**: Open folder with Java Extension Pack

### Hot Reload

Spring Boot DevTools is included for automatic restart during development.

---

## 📝 License

This project is licensed under the MIT License.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📧 Contact

For questions or support, contact the OmniSolve API Team.

---

<div align="center">

**Built with ❤️ using Spring Boot**

</div>
