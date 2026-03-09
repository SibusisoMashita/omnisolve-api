# Architecture

## System Overview

OmniSolve API is a document control system built with Spring Boot that manages organizational documents with version control, workflow states, and AWS S3 storage integration. The system provides RESTful APIs for managing documents, clauses, departments, and document types with OAuth2 JWT authentication via AWS Cognito.

## Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        Client[Web/Mobile Client]
    end
    
    subgraph "AWS Cloud"
        subgraph "Elastic Beanstalk"
            API[Spring Boot API<br/>Java 21]
        end
        
        subgraph "Data Layer"
            RDS[(PostgreSQL 15<br/>RDS)]
            S3[S3 Bucket<br/>Document Storage]
        end
        
        subgraph "Security"
            Cognito[AWS Cognito<br/>JWT Auth]
        end
    end
    
    subgraph "CI/CD"
        GitHub[GitHub Actions]
        GHCR[GitHub Container<br/>Registry]
    end
    
    Client -->|HTTPS| API
    API -->|SQL| RDS
    API -->|Store/Retrieve Files| S3
    Client -->|Authenticate| Cognito
    Cognito -->|JWT Token| Client
    Client -->|Bearer Token| API
    API -->|Validate JWT| Cognito
    
    GitHub -->|Build & Test| API
    GitHub -->|Push Image| GHCR
    GitHub -->|Deploy JAR| API
    
    style API fill:#4CAF50
    style RDS fill:#2196F3
    style S3 fill:#FF9800
    style Cognito fill:#9C27B0
```

## Component Architecture

### Application Layers

```mermaid
graph LR
    subgraph "Spring Boot Application"
        Controller[Controllers<br/>REST Endpoints]
        Service[Services<br/>Business Logic]
        Repository[Repositories<br/>Data Access]
        Domain[Domain<br/>JPA Entities]
        Config[Configuration<br/>Security, S3, OpenAPI]
    end
    
    Controller --> Service
    Service --> Repository
    Repository --> Domain
    Config -.-> Controller
    Config -.-> Service
    
    style Controller fill:#4CAF50
    style Service fill:#2196F3
    style Repository fill:#FF9800
    style Domain fill:#9C27B0
```

### Core Components

#### 1. Controllers (REST API Layer)
- `HealthController` - Health check endpoint
- `DocumentController` - Document CRUD and workflow operations
- `ClauseController` - ISO clause management
- `DepartmentController` - Department management
- `DocumentTypeController` - Document type management

#### 2. Services (Business Logic Layer)
- `DocumentService` - Document lifecycle, workflow state transitions, S3 integration
- `ClauseService` - Clause operations
- `DepartmentService` - Department operations
- `DocumentTypeService` - Document type operations

#### 3. Repositories (Data Access Layer)
- JPA repositories for database operations
- Spring Data JPA for CRUD operations

#### 4. Domain (Data Model)
- `Document` - Core document entity with metadata
- `DocumentVersion` - Version history with S3 references
- `Clause` - ISO clauses linked to documents
- `Department` - Organizational departments
- `DocumentType` - Document categorization
- `DocumentStatus` - Workflow states
- `AuditLog` - Audit trail for all operations

#### 5. Configuration
- `JwtSecurityConfig` - OAuth2 JWT authentication with Cognito
- `S3Config` - AWS S3 client configuration
- `OpenApiConfig` - Swagger/OpenAPI documentation

## Data Model

```mermaid
erDiagram
    DOCUMENTS ||--o{ DOCUMENT_VERSIONS : has
    DOCUMENTS }o--|| DOCUMENT_TYPES : categorized_by
    DOCUMENTS }o--|| DEPARTMENTS : owned_by
    DOCUMENTS }o--|| DOCUMENT_STATUSES : has_status
    DOCUMENTS }o--o{ CLAUSES : linked_to
    DOCUMENTS ||--o{ DOCUMENT_REVIEWS : reviewed_by
    
    DOCUMENTS {
        uuid id PK
        varchar document_number UK
        varchar title
        varchar summary
        bigint type_id FK
        bigint department_id FK
        bigint status_id FK
        varchar owner_id
        timestamptz next_review_at
        timestamptz created_at
        timestamptz updated_at
    }
    
    DOCUMENT_VERSIONS {
        bigserial id PK
        uuid document_id FK
        int version_number
        varchar s3_key
        varchar file_name
        bigint file_size
        varchar mime_type
        timestamptz uploaded_at
    }
    
    CLAUSES {
        bigserial id PK
        varchar code UK
        varchar title
        varchar description
    }
    
    DEPARTMENTS {
        bigserial id PK
        varchar name UK
        varchar description
    }
    
    DOCUMENT_TYPES {
        bigserial id PK
        varchar name UK
        varchar description
    }
    
    DOCUMENT_STATUSES {
        bigserial id PK
        varchar name UK
    }
```

## Document Workflow

```mermaid
stateDiagram-v2
    [*] --> Draft
    Draft --> Pending: submit()
    Pending --> Active: approve()
    Pending --> Draft: reject()
    Active --> Archived: archive()
    Archived --> [*]
    
    note right of Draft
        Initial state
        Editable
    end note
    
    note right of Pending
        Awaiting approval
        Read-only
    end note
    
    note right of Active
        Approved & published
        Read-only
    end note
    
    note right of Archived
        Retired document
        Read-only
    end note
```

## Security Architecture

### Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Cognito
    participant RDS
    participant S3
    
    Client->>Cognito: Login (username/password)
    Cognito->>Client: JWT Token
    
    Client->>API: Request + Bearer Token
    API->>Cognito: Validate JWT
    Cognito->>API: Token Valid + Claims
    
    API->>RDS: Query Data
    RDS->>API: Results
    
    API->>S3: Store/Retrieve Files
    S3->>API: File Data
    
    API->>Client: Response
```

### Security Features

- **OAuth2 JWT Authentication**: AWS Cognito integration for user authentication
- **Bearer Token Authorization**: All API endpoints (except health check) require valid JWT
- **Audience Validation**: Custom validator ensures JWT audience matches expected client ID
- **Configurable Security**: JWT can be disabled for local development
- **IAM Roles**: EC2 instances use IAM roles for S3 access (no hardcoded credentials)

## AWS Infrastructure

```mermaid
graph TB
    subgraph "VPC"
        subgraph "Public Subnets"
            EB[Elastic Beanstalk<br/>EC2 Instance]
        end
        
        subgraph "Private Subnets"
            RDS[(RDS PostgreSQL)]
        end
        
        SG_EB[Security Group<br/>Beanstalk]
        SG_RDS[Security Group<br/>RDS]
    end
    
    S3_Docs[S3 Bucket<br/>Documents]
    S3_Deploy[S3 Bucket<br/>Deployments]
    
    Internet[Internet] -->|HTTP/HTTPS| EB
    EB -->|Port 5432| RDS
    EB -->|S3 API| S3_Docs
    EB -.->|IAM Role| S3_Docs
    
    SG_EB -.->|Allow 5432| SG_RDS
    
    style EB fill:#4CAF50
    style RDS fill:#2196F3
    style S3_Docs fill:#FF9800
```

### Infrastructure Components

- **Elastic Beanstalk**: Single-instance Java application platform
- **RDS PostgreSQL 15**: Managed database with automated backups
- **S3 Buckets**: 
  - Document storage with versioning and encryption
  - Deployment artifacts storage
- **VPC**: Network isolation with public/private subnets
- **Security Groups**: Network access control
- **IAM Roles**: Service permissions without credentials

## CI/CD Pipeline

```mermaid
graph LR
    A[Code Push] --> B[Build]
    B --> C[Test]
    C --> D[Quality Check<br/>Qodana]
    D --> E[Docker Build]
    E --> F[Deploy DEV]
    F --> G{Manual<br/>Approval}
    G -->|Approved| H[Deploy PROD]
    
    style A fill:#9E9E9E
    style B fill:#4CAF50
    style C fill:#2196F3
    style D fill:#FF9800
    style E fill:#9C27B0
    style F fill:#00BCD4
    style G fill:#FFC107
    style H fill:#F44336
```

### Pipeline Stages

1. **Build**: Maven compilation and packaging
2. **Test**: Unit and integration tests with PostgreSQL
3. **Quality**: Qodana code quality analysis
4. **Docker**: Build and push container images to GHCR
5. **Deploy DEV**: Automatic deployment to development environment
6. **Deploy PROD**: Manual approval required for production

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.3.5
- **Language**: Java 21
- **Security**: Spring Security + OAuth2 Resource Server
- **Database**: PostgreSQL 15
- **ORM**: Spring Data JPA + Hibernate
- **Migrations**: Flyway
- **Cloud SDK**: AWS SDK for Java (S3)

### Infrastructure
- **IaC**: Terraform
- **Cloud**: AWS (Elastic Beanstalk, RDS, S3, Cognito)
- **CI/CD**: GitHub Actions
- **Container**: Docker
- **Registry**: GitHub Container Registry (GHCR)

### Development
- **Build Tool**: Maven
- **API Docs**: SpringDoc OpenAPI 3 (Swagger UI)
- **Testing**: JUnit 5, Testcontainers, Embedded PostgreSQL
- **Code Quality**: JetBrains Qodana

## Deployment Architecture

### Environment Separation

| Environment | Purpose | Deployment | Database | S3 Bucket |
|-------------|---------|------------|----------|-----------|
| **Local** | Development | Manual | Docker Compose | Local bucket |
| **DEV** | Testing | Automatic on push to main | RDS t3.micro | dev-documents |
| **PROD** | Production | Manual approval | RDS t3.micro | prod-documents |

### Configuration Management

- **Environment Variables**: Injected via Elastic Beanstalk environment settings
- **Spring Profiles**: `local`, `dev`, `prod`
- **Secrets**: Stored in GitHub Secrets and AWS Parameter Store
- **Terraform State**: Remote backend in S3 with DynamoDB locking

## Scalability Considerations

### Current Architecture
- Single-instance Elastic Beanstalk (suitable for small-medium workloads)
- Single-AZ RDS (cost-optimized)
- S3 for file storage (inherently scalable)

### Future Scaling Options
- **Horizontal Scaling**: Switch to load-balanced Elastic Beanstalk environment
- **Database**: Enable Multi-AZ RDS for high availability
- **Caching**: Add Redis/ElastiCache for session and query caching
- **CDN**: CloudFront for S3 document delivery
- **Async Processing**: SQS + Lambda for background tasks

## Monitoring and Observability

### Health Checks
- **Endpoint**: `/api/health`
- **Elastic Beanstalk**: Monitors application health
- **CI/CD**: Post-deployment health verification

### Logging
- **Application Logs**: Spring Boot logging to stdout
- **Elastic Beanstalk**: Aggregates logs to CloudWatch
- **Audit Trail**: Database audit_logs table for all operations

### Metrics
- **Elastic Beanstalk**: CPU, memory, network metrics
- **RDS**: Database performance metrics
- **S3**: Storage and request metrics
