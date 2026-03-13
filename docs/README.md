# OmniSolve API Documentation

Welcome to the OmniSolve API documentation. This directory contains comprehensive documentation for the entire system.

## Quick Navigation

### New to OmniSolve?
Start here to understand the system:
- **[System Overview](./system-overview.md)** - Complete introduction to OmniSolve capabilities and architecture

### Architecture & Design
Understand how the system is built:
- **[Architecture](./architecture.md)** - High-level system design, layered architecture, and design patterns
- **[Backend Structure](./backend-structure.md)** - Package organization, naming conventions, and code structure
- **[Database](./database.md)** - Schema design, migrations, indexing strategy, and query optimization
- **[Multi-Tenancy](./multi-tenancy.md)** - Tenant isolation, data scoping, and security guarantees
- **[Security](./security.md)** - Authentication, JWT validation, RBAC, and security best practices

### Feature Modules
Deep dive into each business module:
- **[Document Control](./document-module.md)** - Document lifecycle, versioning, ISO clause linking, and review tracking
- **[Incident Management](./incident-module.md)** - Incident tracking, investigation, corrective actions, and timeline
- **[Contractor Management](./contractor-module.md)** - Contractor compliance, document expiry, workers, and site access
- **[Asset Inspections](./assurance-module.md)** - Asset management, inspection checklists, findings, and attachments

### Development & Operations
Practical guides for working with the system:
- **[Development Guide](./development.md)** - Local setup, running tests, debugging, and contributing
- **[API Endpoints](./api-endpoints.md)** - Complete REST API reference with examples
- **[AWS Infrastructure](./aws-infrastructure.md)** - Cloud architecture, Terraform, deployment, and monitoring

## Documentation by Role

### For Developers
1. Start with [System Overview](./system-overview.md) to understand capabilities
2. Read [Architecture](./architecture.md) to understand design decisions
3. Review [Backend Structure](./backend-structure.md) to navigate the codebase
4. Follow [Development Guide](./development.md) to set up your environment
5. Explore [Database](./database.md) to understand data models
6. Study individual module docs for detailed implementation

### For DevOps/Infrastructure
1. Review [AWS Infrastructure](./aws-infrastructure.md) for deployment architecture
2. Understand [Security](./security.md) for authentication and authorization
3. Check [Database](./database.md) for backup and recovery strategies
4. Review [Development Guide](./development.md) for environment configuration

### For API Consumers
1. Start with [API Endpoints](./api-endpoints.md) for complete API reference
2. Review [Security](./security.md) for authentication requirements
3. Understand [Multi-Tenancy](./multi-tenancy.md) for data isolation
4. Check individual module docs for business logic details

### For Product Managers
1. Read [System Overview](./system-overview.md) for capabilities and features
2. Review module docs to understand workflows:
   - [Document Control](./document-module.md)
   - [Incident Management](./incident-module.md)
   - [Contractor Management](./contractor-module.md)
   - [Asset Inspections](./assurance-module.md)
3. Check [Architecture](./architecture.md) for scalability and extensibility

## Key Concepts

### Multi-Tenancy
OmniSolve uses a shared database, shared schema approach where:
- Each organization is a separate tenant
- Data is isolated via `organisation_id` foreign keys
- Reference data is shared across all tenants
- Queries automatically filter by tenant context

### Authentication Flow
1. User authenticates with AWS Cognito
2. Cognito returns JWT token
3. Client includes token in Authorization header
4. API validates token and extracts user identity
5. System resolves organization from employee record
6. All queries automatically filter by organization

### Module Architecture
Each module follows a consistent pattern:
- **Controller**: REST endpoints
- **Service**: Business logic
- **Repository**: Data access
- **Domain**: JPA entities
- **DTO**: Request/response objects

### Audit Trail
All operations are audited:
- Who performed the action
- What entity was affected
- When it happened
- Details in JSONB format
- Organization-scoped for compliance

## Common Tasks

### Adding a New Feature
1. Create domain entity in appropriate module
2. Create repository interface
3. Create DTOs for requests/responses
4. Implement service with business logic
5. Create controller with REST endpoints
6. Add database migration
7. Write tests (unit + integration)
8. Update API documentation

### Adding a New Module
1. Create module package structure
2. Define domain entities
3. Create repositories
4. Implement services
5. Create controllers
6. Add database migration
7. Update architecture documentation
8. Add module-specific documentation

### Debugging Issues
1. Check application logs in CloudWatch
2. Review audit logs in database
3. Use Swagger UI to test endpoints
4. Check database queries with EXPLAIN ANALYZE
5. Review security context and tenant filtering
6. Verify JWT token claims

### Deploying Changes
1. Push code to GitHub
2. CI/CD pipeline runs tests
3. Docker image built
4. Deployed to Elastic Beanstalk
5. Health checks validated
6. Monitor CloudWatch for errors

## Technology Stack Summary

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Backend Framework | Spring Boot 3.3.5 | Application framework |
| Language | Java 21 | Programming language |
| Database | PostgreSQL 16 | Relational data storage |
| Migrations | Flyway | Database version control |
| Authentication | AWS Cognito | User management |
| Storage | AWS S3 | Document/attachment storage |
| Hosting | AWS Elastic Beanstalk | Application hosting |
| Infrastructure | Terraform | Infrastructure as code |
| API Docs | SpringDoc OpenAPI | API documentation |
| Testing | JUnit + Embedded PostgreSQL | Automated testing |

## Module Summary

| Module | Purpose | Key Entities | Status |
|--------|---------|--------------|--------|
| Document Control | Manage controlled documents | Document, DocumentVersion | ✅ Active |
| Incident Management | Track workplace incidents | Incident, Investigation, Action | ✅ Active |
| Contractor Management | Contractor compliance | Contractor, Worker, Document | ✅ Active |
| Asset Inspections | Inspect physical assets | Asset, Inspection, Finding | ✅ Active |

## Getting Help

### Documentation Issues
If you find errors or gaps in the documentation:
1. Check if the information exists in another document
2. Review the source code for clarification
3. Contact the development team
4. Submit a documentation update

### Technical Questions
For technical questions:
1. Review relevant documentation sections
2. Check API documentation and examples
3. Review test cases for usage examples
4. Contact the development team

### Feature Requests
For new features or enhancements:
1. Review [System Overview](./system-overview.md) for planned features
2. Check if similar functionality exists
3. Document the use case and requirements
4. Discuss with product management

## Contributing to Documentation

When updating documentation:
1. Keep it concise and actionable
2. Include code examples where helpful
3. Use diagrams for complex concepts
4. Update related documents
5. Maintain consistent formatting
6. Test all code examples
7. Update the table of contents

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-03 | Initial comprehensive documentation |
| - | - | Added all four modules |
| - | - | Complete API reference |
| - | - | Architecture and design docs |
| - | - | Development and deployment guides |

---

**Maintained by**: [Valo Systems](https://valosystems.co.za/)  
**Last Updated**: March 2024  
**Status**: Current and Complete
