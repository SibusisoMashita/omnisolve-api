# Local Development Guide

## Prerequisites

### Required Software

- **Java 21** (JDK)
  - Download: [Eclipse Temurin](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
  - Verify: `java -version`

- **Maven** (included via Maven Wrapper)
  - No separate installation needed
  - Uses `./mvnw` (Unix) or `mvnw.cmd` (Windows)

- **Docker & Docker Compose**
  - Download: [Docker Desktop](https://www.docker.com/products/docker-desktop/)
  - Verify: `docker --version` and `docker-compose --version`

### Optional Software

- **PostgreSQL Client** (for database management)
  - Download: [pgAdmin](https://www.pgadmin.org/) or [DBeaver](https://dbeaver.io/)

- **AWS CLI** (for S3 testing)
  - Download: [AWS CLI](https://aws.amazon.com/cli/)
  - Configure: `aws configure`

- **Postman or Insomnia** (for API testing)
  - Download: [Postman](https://www.postman.com/) or [Insomnia](https://insomnia.rest/)

---

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/SibusisoMashita/omnisolve-api.git
cd omnisolve-api
```

### 2. Start PostgreSQL

```bash
# Start PostgreSQL with Docker Compose
docker-compose up -d postgres

# Verify PostgreSQL is running
docker-compose ps
```

### 3. Run Application

```bash
# Run with Maven Wrapper
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

### 4. Verify Application

Open browser and navigate to:
- **API Health**: http://localhost:8080/api/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html

Expected health response:
```json
{"status":"ok"}
```

---

## Environment Setup

### Database Configuration

#### Using Docker Compose (Recommended)

The `docker-compose.yml` file provides a PostgreSQL 16 instance:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: omnisolve
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
```

**Start database**:
```bash
docker-compose up -d postgres
```

**Stop database**:
```bash
docker-compose down
```

**View logs**:
```bash
docker-compose logs -f postgres
```

#### Using Local PostgreSQL

If you have PostgreSQL installed locally:

1. Create database:
   ```sql
   CREATE DATABASE omnisolve;
   CREATE USER postgres WITH PASSWORD 'admin';
   GRANT ALL PRIVILEGES ON DATABASE omnisolve TO postgres;
   ```

2. Update connection in `application-local.yml` if needed

### Application Configuration

#### Default Configuration

The application uses `application-local.yml` profile by default:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/omnisolve
    username: postgres
    password: admin
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_example

app:
  s3:
    bucket: dev-omnisolve-documents
  security:
    jwt:
      enabled: false  # JWT disabled for local development
```

#### Custom Configuration

Create `application-local.yml` in `src/main/resources/` to override defaults:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/my_custom_db
    username: myuser
    password: mypassword

app:
  s3:
    bucket: my-local-bucket
```

#### Environment Variables

Override configuration using environment variables:

```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/omnisolve
export DB_USERNAME=postgres
export DB_PASSWORD=admin

# S3
export AWS_REGION=us-east-1
export DOCUMENT_BUCKET=dev-omnisolve-documents

# Security
export JWT_ENABLED=false

# CORS (for frontend development)
export CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# Run application
./mvnw spring-boot:run
```

---

## Running the Application

### Maven Wrapper

#### Run Application

```bash
# Unix/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

#### Build JAR

```bash
# Build without tests
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package

# Run JAR
java -jar target/omnisolve-api-0.0.1.jar
```

#### Clean Build

```bash
# Clean and rebuild
./mvnw clean install
```

### Docker Compose (Full Stack)

Run both PostgreSQL and the application:

```bash
# Build and start all services
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop all services
docker-compose down
```

### IDE Integration

#### IntelliJ IDEA

1. Open project in IntelliJ
2. Wait for Maven import to complete
3. Right-click `OmnisolveApiApplication.java`
4. Select "Run 'OmnisolveApiApplication'"

**Run Configuration**:
- Main class: `com.omnisolve.OmnisolveApiApplication`
- VM options: `-Dspring.profiles.active=local`
- Environment variables: 
  - `JWT_ENABLED=false`
  - `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000`

#### VS Code

1. Install "Extension Pack for Java"
2. Open project folder
3. Press F5 or use "Run and Debug" panel
4. Select "Spring Boot App"

**launch.json**:
```json
{
  "type": "java",
  "name": "OmniSolve API",
  "request": "launch",
  "mainClass": "com.omnisolve.OmnisolveApiApplication",
  "projectName": "omnisolve-api",
  "env": {
    "SPRING_PROFILES_ACTIVE": "local",
    "JWT_ENABLED": "false",
    "CORS_ALLOWED_ORIGINS": "http://localhost:5173,http://localhost:3000"
  }
}
```

#### Eclipse

1. Import as Maven project
2. Right-click project → Run As → Spring Boot App

---

## Testing

### Run All Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Skip tests
./mvnw package -DskipTests
```

### Run Specific Tests

```bash
# Run specific test class
./mvnw test -Dtest=DocumentServiceTest

# Run specific test method
./mvnw test -Dtest=DocumentServiceTest#testCreateDocument
```

### Test Configuration

Tests use embedded PostgreSQL (no Docker required):

```xml
<dependency>
  <groupId>io.zonky.test</groupId>
  <artifactId>embedded-postgres</artifactId>
  <version>2.0.7</version>
  <scope>test</scope>
</dependency>
```

Alternatively, tests can use Testcontainers:

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

### Integration Tests

Integration tests run against a real database:

```bash
# Start test database
docker-compose up -d postgres

# Run integration tests
./mvnw verify -Pintegration-tests
```

---

## Database Management

### Flyway Migrations

Migrations are located in `src/main/resources/db/migration/`:

```
db/migration/
├── V1__init.sql           # Initial schema
├── V2__seed_data.sql      # Seed data
└── V3__seed_documents.sql # Sample documents
```

#### Run Migrations

Migrations run automatically on application startup.

#### Manual Migration

```bash
# Run migrations
./mvnw flyway:migrate

# Show migration status
./mvnw flyway:info

# Clean database (use with caution)
./mvnw flyway:clean
```

### Database Access

#### Using psql

```bash
# Connect to database
docker exec -it omnisolve-postgres psql -U postgres -d omnisolve

# Run queries
SELECT * FROM documents;
SELECT * FROM document_types;

# Exit
\q
```

#### Using pgAdmin

1. Open pgAdmin
2. Add new server:
   - Host: `localhost`
   - Port: `5432`
   - Database: `omnisolve`
   - Username: `postgres`
   - Password: `admin`

#### Using DBeaver

1. Open DBeaver
2. New Connection → PostgreSQL
3. Connection settings:
   - Host: `localhost`
   - Port: `5432`
   - Database: `omnisolve`
   - Username: `postgres`
   - Password: `admin`

---

## API Testing

### Swagger UI

Navigate to http://localhost:8080/swagger-ui.html

**Features**:
- Interactive API documentation
- Try out endpoints directly
- View request/response schemas
- No authentication required (local mode)

### cURL Examples

#### Health Check

```bash
curl http://localhost:8080/api/health
```

#### List Documents

```bash
curl http://localhost:8080/api/documents
```

#### Create Document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "documentNumber": "DOC-001",
    "title": "Test Document",
    "summary": "Test summary",
    "typeId": 1,
    "departmentId": 1,
    "ownerId": "user123",
    "clauseIds": [1, 2]
  }'
```

#### Upload Document Version

```bash
curl -X POST http://localhost:8080/api/documents/{id}/versions \
  -F "file=@document.pdf"
```

### Postman Collection

Create a Postman collection with these requests:

1. **Health Check**: `GET http://localhost:8080/api/health`
2. **List Documents**: `GET http://localhost:8080/api/documents`
3. **Create Document**: `POST http://localhost:8080/api/documents`
4. **Get Document**: `GET http://localhost:8080/api/documents/{id}`
5. **Submit Document**: `POST http://localhost:8080/api/documents/{id}/submit`
6. **Approve Document**: `POST http://localhost:8080/api/documents/{id}/approve`

---

## S3 Local Development

### Option 1: LocalStack (Recommended)

LocalStack provides a local AWS cloud stack:

```bash
# Install LocalStack
pip install localstack

# Start LocalStack
localstack start

# Create local S3 bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://dev-omnisolve-documents

# Configure application
export AWS_ENDPOINT_URL=http://localhost:4566
```

### Option 2: MinIO

MinIO is an S3-compatible object storage:

```bash
# Run MinIO with Docker
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"

# Access MinIO Console: http://localhost:9001
# Create bucket: dev-omnisolve-documents
```

### Option 3: Real AWS S3

Use a real S3 bucket for local development:

```bash
# Configure AWS credentials
aws configure

# Create bucket
aws s3 mb s3://dev-omnisolve-documents

# Update application configuration
export DOCUMENT_BUCKET=dev-omnisolve-documents
```

---

## CORS Configuration

### Overview

The application includes CORS (Cross-Origin Resource Sharing) support for frontend integration. CORS is configured via `CorsConfig.java` and allows cross-origin requests from specified origins.

### Configuration

CORS settings are controlled by the `CORS_ALLOWED_ORIGINS` environment variable:

**Default (Local)**:
```yaml
app:
  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000,http://localhost:8080
```

**Custom Origins**:
```bash
export CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:4200
./mvnw spring-boot:run
```

### Allowed Configuration

- **Paths**: `/api/**` (all API endpoints)
- **Methods**: GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Headers**: All headers allowed (`*`)
- **Credentials**: Enabled (`allowCredentials: true`)
- **Max Age**: 3600 seconds (1 hour preflight cache)

### Testing CORS

#### Test Preflight Request

```bash
curl -X OPTIONS http://localhost:8080/api/documents \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

#### Test Actual Request

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Origin: http://localhost:5173" \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Credentials: true
Content-Type: application/json
```

### Frontend Integration

#### Fetch API

```javascript
// Fetch with credentials
fetch('http://localhost:8080/api/documents', {
  method: 'GET',
  credentials: 'include',  // Important for cookies/auth
  headers: {
    'Content-Type': 'application/json',
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

#### Axios

```javascript
// Configure Axios
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true,  // Important for cookies/auth
});

// Make request
api.get('/api/documents')
  .then(response => console.log(response.data));
```

### Common Issues

**1. Origin Not Allowed**

Error: `Access to fetch at 'http://localhost:8080/api/documents' from origin 'http://localhost:5173' has been blocked by CORS policy`

Solution: Add your frontend URL to `CORS_ALLOWED_ORIGINS`:
```bash
export CORS_ALLOWED_ORIGINS=http://localhost:5173
```

**2. Credentials Error**

Error: `The value of the 'Access-Control-Allow-Credentials' header in the response is '' which must be 'true'`

Solution: Ensure `withCredentials: true` (Axios) or `credentials: 'include'` (Fetch) is set in frontend.

**3. Preflight Fails**

Error: `Response to preflight request doesn't pass access control check`

Solution: 
- Verify OPTIONS method is allowed
- Check `Access-Control-Request-Headers` matches allowed headers
- Ensure origin is in allowed origins list

### Production Configuration

For production, set specific frontend URLs:

**DEV Environment**:
```bash
export CORS_ALLOWED_ORIGINS=https://dev-app.example.com
```

**PROD Environment**:
```bash
export CORS_ALLOWED_ORIGINS=https://app.example.com
```

**Multiple Origins**:
```bash
export CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

### Security Considerations

- Never use wildcard (`*`) origins in production
- Always specify exact origins
- Keep `allowCredentials: true` for authenticated requests
- Use HTTPS in production
- Limit allowed methods to what's actually needed

---

## Debugging

### Enable Debug Logging

Add to `application-local.yml`:

```yaml
logging:
  level:
    com.omnisolve: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Remote Debugging

#### IntelliJ IDEA

1. Run → Edit Configurations
2. Add "Remote JVM Debug"
3. Port: 5005
4. Start application with debug flag:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
   ```
5. Start debugger in IntelliJ

#### VS Code

Add to `launch.json`:
```json
{
  "type": "java",
  "name": "Attach to Remote",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

---

## Hot Reload

### Spring Boot DevTools

Add to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
```

**Features**:
- Automatic restart on code changes
- LiveReload support
- Property defaults for development

### IDE Configuration

#### IntelliJ IDEA

1. Settings → Build, Execution, Deployment → Compiler
2. Enable "Build project automatically"
3. Settings → Advanced Settings
4. Enable "Allow auto-make to start even if developed application is currently running"

#### VS Code

DevTools works automatically with Java Extension Pack.

---

## Troubleshooting

### Port Already in Use

**Problem**: Port 8080 is already in use

**Solution**:
```bash
# Find process using port 8080
# Unix/macOS
lsof -i :8080

# Windows
netstat -ano | findstr :8080

# Kill process or change port
export SERVER_PORT=8081
./mvnw spring-boot:run
```

### Database Connection Failed

**Problem**: Cannot connect to PostgreSQL

**Solution**:
1. Verify PostgreSQL is running: `docker-compose ps`
2. Check connection settings in `application-local.yml`
3. Verify port 5432 is not blocked
4. Check Docker logs: `docker-compose logs postgres`

### Flyway Migration Failed

**Problem**: Migration fails on startup

**Solution**:
```bash
# Clean database and restart
docker-compose down -v
docker-compose up -d postgres
./mvnw spring-boot:run
```

### Maven Build Failed

**Problem**: Maven build fails

**Solution**:
```bash
# Clean Maven cache
./mvnw clean

# Update dependencies
./mvnw dependency:purge-local-repository

# Rebuild
./mvnw clean install
```

### S3 Access Denied

**Problem**: Cannot upload files to S3

**Solution**:
1. Verify AWS credentials: `aws sts get-caller-identity`
2. Check bucket permissions
3. Verify bucket name is correct
4. For local development, disable JWT: `JWT_ENABLED=false`

---

## Best Practices

### Code Style

- Follow Java naming conventions
- Use Spring Boot best practices
- Write unit tests for new features
- Document public APIs with Javadoc

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes and commit
git add .
git commit -m "feat: add new feature"

# Push and create PR
git push origin feature/my-feature
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `test:` Test changes
- `refactor:` Code refactoring
- `chore:` Maintenance tasks

### Testing

- Write tests for new features
- Maintain test coverage above 80%
- Use meaningful test names
- Test edge cases and error conditions

---

## Useful Commands

### Maven

```bash
# Clean build
./mvnw clean install

# Run tests
./mvnw test

# Skip tests
./mvnw package -DskipTests

# Run specific test
./mvnw test -Dtest=DocumentServiceTest

# Check dependencies
./mvnw dependency:tree

# Update dependencies
./mvnw versions:display-dependency-updates
```

### Docker

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Rebuild images
docker-compose up --build

# Remove volumes
docker-compose down -v
```

### Database

```bash
# Connect to database
docker exec -it omnisolve-postgres psql -U postgres -d omnisolve

# Backup database
docker exec omnisolve-postgres pg_dump -U postgres omnisolve > backup.sql

# Restore database
docker exec -i omnisolve-postgres psql -U postgres omnisolve < backup.sql
```
