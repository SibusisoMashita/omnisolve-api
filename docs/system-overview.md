# OmniSolve System Overview

## Introduction

OmniSolve is a comprehensive multi-tenant SaaS platform designed for compliance management across multiple ISO standards (ISO 9001, ISO 14001, ISO 45001). The system provides integrated modules for document control, incident management, contractor compliance, and asset inspections, enabling organizations to maintain regulatory compliance and operational excellence.

## System Capabilities

### Document Control
- Manage policies, procedures, manuals, and work instructions
- Workflow-based approval process (Draft → Pending → Active → Archived)
- Version control with S3 storage
- ISO clause linking for compliance mapping
- Review tracking and due date monitoring
- Automatic document numbering per organization
- Dashboard with attention items and statistics

### Incident Management
- Track workplace incidents from report to closure
- Support for injury, environmental, quality, security, and near-miss incidents
- Investigation management with root cause analysis
- Corrective action tracking with assignments and due dates
- Timeline comments for incident history
- Attachment support for evidence and photos
- Severity-based prioritization
- Dashboard metrics and analytics

### Contractor Management
- Manage external contractor companies and workers
- Track compliance documentation (insurance, COIDA, safety files)
- Monitor document expiry dates with alerts
- Site access control and permissions
- Compliance dashboard showing status across contractors
- Worker registration and tracking
- Document upload with S3 storage

### Asset Inspection & Assurance
- Register and track inspectable assets (equipment, facilities, vehicles)
- Create reusable inspection checklist templates
- Conduct inspections with structured checklists
- Record findings with severity levels
- Link findings to ISO clauses
- Attach photos and documents to inspections
- Track inspection status (Scheduled → In Progress → Completed)
- Optional tagging system for categorization

## Technical Architecture

### Technology Stack
- **Backend Framework**: Spring Boot 3.3.5 with Java 21
- **Database**: PostgreSQL 16 with Flyway migrations
- **Authentication**: AWS Cognito with JWT validation
- **Storage**: AWS S3 for documents and attachments
- **Hosting**: AWS Elastic Beanstalk with auto-scaling
- **Infrastructure**: Terraform for infrastructure as code
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: JUnit with embedded PostgreSQL

### Multi-Tenancy Model
- **Shared Database, Shared Schema** approach
- Complete data isolation via `organisation_id` foreign keys
- Automatic tenant filtering in all queries
- Shared reference data (document types, ISO clauses, etc.)
- Per-tenant roles and permissions

### Security Model
- JWT-based authentication via AWS Cognito
- Token validation on every request
- Role-based access control (RBAC)
- Organization-scoped permissions
- Audit logging for all operations
- HTTPS-only communication in production

### Data Architecture
- **UUID Primary Keys**: For business entities (documents, incidents, assets, contractors)
- **BIGSERIAL Primary Keys**: For reference data and metadata
- **Composite Indexes**: Optimized for multi-tenant queries
- **Foreign Key Constraints**: Enforce referential integrity
- **Cascade Deletes**: Automatic cleanup when organizations are deleted

## Module Integration

### Shared Infrastructure

All modules share common infrastructure:

**Organizations & Sites**
- Root tenant entity
- Physical location management
- Site-based access control

**Employees**
- Linked to AWS Cognito users
- Organization-scoped
- Role assignments for RBAC

**Departments**
- Shared across all modules
- Used for categorization and filtering

**ISO Standards & Clauses**
- Multi-standard support (ISO 9001, 14001, 45001)
- Hierarchical clause structure
- Linkable from documents and inspection findings

**Audit Logs**
- Comprehensive audit trail
- JSONB details for flexibility
- Organization-scoped

### Cross-Module Workflows

**Incident → Corrective Action → Document**
1. Incident identified and investigated
2. Corrective action created
3. New procedure document created to prevent recurrence
4. Document linked to relevant ISO clause

**Inspection → Finding → Incident**
1. Asset inspection conducted
2. Critical finding recorded
3. Incident created for investigation
4. Corrective action assigned

**Contractor → Site Access → Inspection**
1. Contractor compliance verified
2. Site access granted
3. Contractor equipment inspected
4. Inspection findings tracked

## API Design

### RESTful Principles
- Resource-based URLs (`/api/documents`, `/api/incidents`)
- HTTP verbs for operations (GET, POST, PUT, DELETE)
- JSON request/response format
- Consistent error responses
- Pagination support for lists

### Authentication
- Bearer token in Authorization header
- JWT validation on every request
- User context resolution from token
- Automatic tenant filtering

### Response Formats

**Success Response:**
```json
{
  "id": "uuid",
  "field1": "value1",
  "field2": "value2",
  "createdAt": "2024-03-12T10:30:00Z"
}
```

**Error Response:**
```json
{
  "timestamp": "2024-03-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/documents"
}
```

**Paginated Response:**
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

## Storage Strategy

### S3 Bucket Organization

```
{environment}-omnisolve-documents/
├── documents/
│   └── {document-id}/
│       ├── v1/{filename}
│       ├── v2/{filename}
│       └── v3/{filename}
├── incidents/
│   └── {incident-id}/
│       └── {uuid}/{filename}
├── inspections/
│   └── {inspection-id}/
│       └── {uuid}/{filename}
└── contractors/
    └── {contractor-id}/
        └── {document-type-id}/
            └── {uuid}/{filename}
```

### Storage Features
- Versioning enabled
- Server-side encryption (AES-256)
- Lifecycle policies for archival
- Pre-signed URLs for downloads
- IAM-based access control

## Deployment Architecture

### AWS Infrastructure

```
┌─────────────────────────────────────────────────┐
│                  AWS Cloud                       │
│                                                  │
│  ┌──────────────┐         ┌─────────────────┐  │
│  │   Cognito    │         │  CloudFront     │  │
│  │  User Pool   │         │  (Optional CDN) │  │
│  └──────────────┘         └─────────────────┘  │
│         │                          │            │
│         │                          │            │
│  ┌──────▼──────────────────────────▼─────────┐ │
│  │     Application Load Balancer (ALB)       │ │
│  └──────────────────┬─────────────────────────┘ │
│                     │                            │
│  ┌──────────────────▼─────────────────────────┐ │
│  │      Elastic Beanstalk Environment        │ │
│  │  ┌────────────┐      ┌────────────┐       │ │
│  │  │ EC2 Instance│      │ EC2 Instance│      │ │
│  │  │ Spring Boot │      │ Spring Boot │      │ │
│  │  └────────────┘      └────────────┘       │ │
│  └──────────────────┬─────────────────────────┘ │
│                     │                            │
│         ┌───────────┴───────────┐                │
│         │                       │                │
│  ┌──────▼──────┐         ┌─────▼──────┐         │
│  │     RDS     │         │     S3     │         │
│  │ PostgreSQL  │         │   Bucket   │         │
│  │  Multi-AZ   │         │ Documents  │         │
│  └─────────────┘         └────────────┘         │
└─────────────────────────────────────────────────┘
```

### Environments
- **Development**: Single instance, t3.small, dev database
- **Production**: Auto-scaling (1-4 instances), t3.medium, Multi-AZ RDS

### CI/CD Pipeline
1. Code pushed to GitHub
2. GitHub Actions triggered
3. Tests executed (unit + integration)
4. Docker image built
5. Deployed to Elastic Beanstalk
6. Health checks validated
7. Rollback on failure

## Monitoring & Observability

### Logging
- Application logs to CloudWatch
- Structured logging with MDC (user ID, tenant ID)
- Request/response logging filter
- Audit logs in database

### Metrics
- Application metrics (request count, response time)
- Infrastructure metrics (CPU, memory, disk)
- Database metrics (connections, query time)
- S3 metrics (request count, data transfer)

### Alerts
- High CPU utilization
- High database connections
- Application errors (5xx responses)
- Health check failures

## Compliance Features

### ISO Standard Support
- ISO 9001:2015 (Quality Management)
- ISO 14001:2015 (Environmental Management)
- ISO 45001:2018 (Occupational Health & Safety)

### Compliance Capabilities
- Document control with approval workflows
- ISO clause linking and mapping
- Incident tracking and investigation
- Corrective action management
- Contractor compliance monitoring
- Asset inspection and findings
- Comprehensive audit trail
- Review tracking and alerts

### Audit Trail
- All operations logged
- Immutable audit records
- JSONB details for flexibility
- Queryable by entity, action, user, date
- Organization-scoped

## Performance Optimization

### Caching Strategy
- In-memory cache (Caffeine)
- Cache document statistics
- Cache incident dashboard
- Per-tenant cache keys
- TTL: 5 minutes
- Eviction on write operations

### Database Optimization
- Composite indexes for multi-tenant queries
- Lazy loading for relationships
- Read-only transactions for queries
- Connection pooling (HikariCP)
- Query optimization with EXPLAIN ANALYZE

### Scalability
- Stateless API design
- Horizontal scaling via Elastic Beanstalk
- Database connection pooling
- Asynchronous processing for audit logs
- Event-driven architecture for side effects

## Future Enhancements

### Planned Features
- Risk management module
- Training management module
- Supplier management
- Advanced reporting and analytics
- Mobile application
- Offline inspection capability
- Integration APIs for third-party systems
- Advanced workflow engine
- Document collaboration features
- Real-time notifications

### Technical Improvements
- Read replicas for reporting
- ElastiCache for distributed caching
- SQS for asynchronous processing
- Lambda functions for background jobs
- GraphQL API option
- WebSocket support for real-time updates

## Getting Started

### For Developers
1. Read [Development Guide](./development.md)
2. Set up local environment
3. Review [Architecture](./architecture.md)
4. Explore [API Endpoints](./api-endpoints.md)
5. Understand [Multi-Tenancy](./multi-tenancy.md)

### For Administrators
1. Review [AWS Infrastructure](./aws-infrastructure.md)
2. Configure Terraform variables
3. Deploy infrastructure
4. Set up Cognito user pool
5. Configure environment variables

### For API Consumers
1. Review [API Endpoints](./api-endpoints.md)
2. Obtain JWT token from Cognito
3. Make authenticated requests
4. Handle pagination and errors
5. Follow rate limits

## Support & Resources

### Documentation
- [Architecture](./architecture.md) - System design and patterns
- [Backend Structure](./backend-structure.md) - Code organization
- [Database](./database.md) - Schema and migrations
- [Security](./security.md) - Authentication and authorization
- [Multi-Tenancy](./multi-tenancy.md) - Tenant isolation
- [Document Module](./document-module.md) - Document control
- [Incident Module](./incident-module.md) - Incident management
- [Contractor Module](./contractor-module.md) - Contractor compliance
- [Assurance Module](./assurance-module.md) - Asset inspections

### Contact
- **Developer**: [Valo Systems](https://valosystems.co.za/)
- **Support**: support@valosystems.co.za
- **Documentation**: This repository's docs/ directory
