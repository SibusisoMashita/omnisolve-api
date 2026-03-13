# Development Guide

## Purpose

This guide explains how to set up a local development environment, run the application, execute tests, and contribute to the OmniSolve API codebase.

## Prerequisites

**Required:**
- Java 21 (OpenJDK or Corretto)
- Maven 3.9+
- PostgreSQL 16 (or Docker)
- Git

**Optional:**
- Docker Desktop (for containerized PostgreSQL)
- IntelliJ IDEA or VS Code
- Postman or similar API testing tool
- AWS CLI (for S3 and Cognito testing)

## Local Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/omnisolve-api.git
cd omnisolve-api
```

### 2. Start PostgreSQL

**Option A: Docker Compose (Recommended)**

```bash
# Start PostgreSQL container
docker-compose up -d

# Verify it's running
docker ps
```

The `docker-compose.yml` file configures:
- PostgreSQL 16
- Port: 5432
- Database: omnisolve
- Username: postgres
- Password: postgres

**Option B: Local PostgreSQL Installation**

```bash
# Create database
createdb omnisolve

# Or using psql
psql -U postgres
CREATE DATABASE omnisolve;
\q
```

### 3. Configure Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/omnisolve
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# AWS (optional for local dev)
export AWS_REGION=us-east-1
export DOCUMENT_BUCKET=dev-omnisolve-documents

# Cognito (optional - can disable JWT)
export COGNITO_AUTHORITY=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_xxxxx
export COGNITO_CLIENT_ID=your-client-id
export COGNITO_USER_POOL_ID=us-east-1_xxxxx

# Security
export JWT_ENABLED=false  # Disable for local dev without Cognito
```

**Development Mode (No Cognito):**

For local development without AWS Cognito, set `JWT_ENABLED=false` in `application.yml`:

```yaml
app:
  security:
    jwt:
      enabled: false
```

This allows all requests without authentication.

### 4. Run Database Migrations

Flyway runs automatically on application startup, but you can verify:

```bash
# Start the application (migrations run automatically)
./mvnw spring-boot:run

# Check flyway_schema_history table
psql -U postgres -d omnisolve -c "SELECT * FROM flyway_schema_history;"
```

### 5. Start the Application

```bash
# Using Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/omnisolve-api.jar
```

The API will be available at `http://localhost:5000`

### 6. Verify Setup

```bash
# Health check
curl http://localhost:5000/api/health

# Expected response:
# {"status":"UP"}

# Swagger UI
open http://localhost:5000/swagger-ui.html
```

## Project Structure

```
omnisolve-api/
├── src/
│   ├── main/
│   │   ├── java/com/omnisolve/
│   │   │   ├── audit/              # Audit logging
│   │   │   ├── config/             # Configuration
│   │   │   ├── controller/         # REST endpoints
│   │   │   ├── domain/             # JPA entities
│   │   │   ├── event/              # Domain events
│   │   │   ├── repository/         # Data access
│   │   │   ├── security/           # Authentication
│   │   │   ├── service/            # Business logic
│   │   │   │   └── dto/            # DTOs
│   │   │   └── tenant/             # Multi-tenancy
│   │   └── resources/
│   │       ├── application.yml     # Configuration
│   │       └── db/migration/       # Flyway migrations
│   └── test/
│       └── java/com/omnisolve/
│           ├── controller/         # Controller tests
│           ├── service/            # Service tests
│           ├── repository/         # Repository tests
│           └── integration/        # Integration tests
├── infrastructure/                 # Terraform IaC
├── scripts/                        # Utility scripts
├── pom.xml                         # Maven dependencies
└── docker-compose.yml              # Local PostgreSQL
```

## Running Tests

### Unit Tests

Unit tests run quickly without external dependencies:

```bash
# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=DocumentServiceTest

# Run specific test method
./mvnw test -Dtest=DocumentServiceTest#testCreateDocument
```

**Test Naming Convention:**
- `{Class}Test.java` - Unit tests
- Example: `DocumentServiceTest.java`

### Integration Tests

Integration tests use embedded PostgreSQL (no Docker required):

```bash
# Run all integration tests
./mvnw verify

# Run specific integration test
./mvnw verify -Dit.test=DocumentControllerIT
```

**Test Naming Convention:**
- `{Class}IT.java` - Integration tests
- Example: `DocumentControllerIT.java`

**Embedded PostgreSQL:**
The project uses `embedded-postgres` for integration tests:

```xml
<dependency>
    <groupId>io.zonky.test</groupId>
    <artifactId>embedded-postgres</artifactId>
    <version>2.0.7</version>
    <scope>test</scope>
</dependency>
```

This automatically starts a PostgreSQL instance for tests (no manual setup needed).

### Test Coverage

```bash
# Generate coverage report
./mvnw clean verify jacoco:report

# View report
open target/site/jacoco/index.html
```

## Testing Strategy

### Unit Tests

Test business logic in isolation:

```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private SecurityContextFacade securityContextFacade;
    
    @InjectMocks
    private DocumentService documentService;
    
    @Test
    void testCreateDocument() {
        // Given
        AuthenticatedUser user = new AuthenticatedUser("user-123", "test@example.com", "testuser", 1L);
        when(securityContextFacade.currentUser()).thenReturn(user);
        
        // When
        DocumentResponse response = documentService.create(request, "user-123");
        
        // Then
        assertNotNull(response);
        verify(documentRepository).save(any(Document.class));
    }
}
```

### Integration Tests

Test full request/response cycle:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DocumentControllerIT {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCreateDocument() {
        // Given
        DocumentRequest request = new DocumentRequest(
            "Quality Policy", 1L, 1L, "Policy summary"
        );
        
        // When
        ResponseEntity<DocumentResponse> response = restTemplate
            .postForEntity("/api/documents", request, DocumentResponse.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
```

### Repository Tests

Test data access layer:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DocumentRepositoryTest {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Test
    void testFindByOrganisationId() {
        // Given
        Document document = createTestDocument(1L);
        documentRepository.save(document);
        
        // When
        List<Document> documents = documentRepository.findByOrganisationId(1L);
        
        // Then
        assertEquals(1, documents.size());
    }
}
```

## API Testing

### Using Swagger UI

1. Start the application
2. Open `http://localhost:5000/swagger-ui.html`
3. Explore endpoints and try requests

### Using cURL

```bash
# Health check
curl http://localhost:5000/api/health

# List documents (JWT disabled)
curl http://localhost:5000/api/documents

# Create document
curl -X POST http://localhost:5000/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Quality Policy",
    "typeId": 1,
    "departmentId": 1,
    "summary": "Our commitment to quality",
    "ownerId": "user-123",
    "createdBy": "user-123"
  }'
```

### Using Postman

Import the OpenAPI spec from `http://localhost:5000/v3/api-docs` into Postman.

## Database Management

### Accessing PostgreSQL

```bash
# Using Docker
docker exec -it omnisolve-postgres psql -U postgres -d omnisolve

# Using local psql
psql -U postgres -d omnisolve
```

### Useful SQL Queries

```sql
-- List all tables
\dt

-- View organisations
SELECT * FROM organisations;

-- View documents
SELECT id, document_number, title, status_id FROM documents;

-- View flyway migrations
SELECT * FROM flyway_schema_history;

-- Count documents by status
SELECT s.name, COUNT(d.id)
FROM documents d
JOIN document_statuses s ON d.status_id = s.id
GROUP BY s.name;
```

### Reset Database

```bash
# Drop and recreate database
psql -U postgres -c "DROP DATABASE omnisolve;"
psql -U postgres -c "CREATE DATABASE omnisolve;"

# Restart application (Flyway will recreate schema)
./mvnw spring-boot:run
```

## Debugging

### IntelliJ IDEA

1. Open project in IntelliJ
2. Set breakpoints in code
3. Run → Debug 'OmnisolveApiApplication'
4. Make API request to trigger breakpoint

### VS Code

1. Install Java Extension Pack
2. Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug OmniSolve API",
      "request": "launch",
      "mainClass": "com.omnisolve.OmnisolveApiApplication",
      "projectName": "omnisolve-api"
    }
  ]
}
```

3. Set breakpoints and press F5

### Logging

Configure logging in `application.yml`:

```yaml
logging:
  level:
    com.omnisolve: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## Common Development Tasks

### Adding a New Entity

1. Create entity class in `domain/`
2. Create repository in `repository/`
3. Create DTOs in `service/dto/`
4. Create service in `service/`
5. Create controller in `controller/`
6. Add Flyway migration in `db/migration/`
7. Write tests

### Adding a New Endpoint

1. Add method to controller:

```java
@GetMapping("/{id}")
public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
    return ResponseEntity.ok(documentService.getDocument(id));
}
```

2. Add method to service:

```java
@Transactional(readOnly = true)
public DocumentResponse getDocument(UUID id) {
    // Implementation
}
```

3. Write tests

### Adding a Database Migration

1. Create new file: `V{version}__{description}.sql`
2. Example: `V6__add_document_tags.sql`

```sql
CREATE TABLE document_tags (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_tags_document_id ON document_tags(document_id);
```

3. Restart application (Flyway runs automatically)

## Code Style

### Java Conventions

- Use Java 21 features (records, pattern matching, etc.)
- Follow Spring Boot best practices
- Use constructor injection (not field injection)
- Prefer immutable DTOs (records)
- Use meaningful variable names

### Formatting

```bash
# Format code (if using Spotless)
./mvnw spotless:apply

# Check formatting
./mvnw spotless:check
```

### Naming Conventions

- Classes: `PascalCase`
- Methods: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 5000
lsof -i :5000

# Kill process
kill -9 <PID>
```

### Database Connection Failed

```bash
# Check PostgreSQL is running
docker ps

# Check connection
psql -U postgres -d omnisolve -c "SELECT 1;"

# Restart PostgreSQL
docker-compose restart
```

### Flyway Migration Failed

```bash
# Check migration history
psql -U postgres -d omnisolve -c "SELECT * FROM flyway_schema_history;"

# Repair Flyway (if needed)
./mvnw flyway:repair

# Or reset database
psql -U postgres -c "DROP DATABASE omnisolve;"
psql -U postgres -c "CREATE DATABASE omnisolve;"
```

### Tests Failing

```bash
# Clean and rebuild
./mvnw clean install

# Run tests with debug logging
./mvnw test -X

# Skip tests temporarily
./mvnw package -DskipTests
```

## Deployment

### Building for Production

```bash
# Build JAR
./mvnw clean package -DskipTests

# JAR location
ls -lh target/omnisolve-api.jar
```

### Deploying to Elastic Beanstalk

```bash
# Initialize EB CLI (first time only)
eb init

# Deploy to environment
eb deploy production

# View logs
eb logs production
```

### Environment Variables (Production)

Set these in Elastic Beanstalk environment configuration:

```
DB_URL=jdbc:postgresql://prod-db.xxxxx.rds.amazonaws.com:5432/omnisolve
DB_USERNAME=omnisolve_app
DB_PASSWORD=<from-secrets-manager>
COGNITO_AUTHORITY=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_xxxxx
COGNITO_CLIENT_ID=<client-id>
DOCUMENT_BUCKET=prod-omnisolve-documents
AWS_REGION=us-east-1
JWT_ENABLED=true
```

## Contributing

### Git Workflow

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes and commit: `git commit -m "Add feature"`
3. Push branch: `git push origin feature/my-feature`
4. Create pull request
5. Wait for code review
6. Merge to main

### Commit Messages

Follow conventional commits:

```
feat: Add incident dashboard endpoint
fix: Resolve document number generation bug
docs: Update API documentation
test: Add integration tests for incidents
refactor: Simplify document service logic
```

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/)
