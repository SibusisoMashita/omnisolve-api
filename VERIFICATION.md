# OmniSolve Backend Verification Guide

This document provides step-by-step instructions to verify that the OmniSolve backend is fully functional.

## Prerequisites

- Java 21 or higher
- Maven 3.6+ (or use the Maven wrapper)
- Docker and Docker Compose
- curl or PowerShell (for API testing)

## Automated Verification

### Option 1: PowerShell Script (Windows)

```powershell
.\verify.ps1
```

### Option 2: Bash Script (Linux/Mac)

```bash
chmod +x verify.sh
./verify.sh
```

## Manual Verification Steps

### 1. Build Verification

Ensure the project compiles successfully.

```bash
# If Maven is installed
mvn clean compile

# Or use Maven wrapper (if available)
./mvnw clean compile  # Linux/Mac
.\mvnw.cmd clean compile  # Windows
```

**Expected Result:** Build completes without errors.

### 2. Docker Environment Setup

Start the PostgreSQL database using Docker Compose.

```bash
docker compose up -d postgres
```

**Expected Result:** PostgreSQL container starts and becomes healthy.

Verify the container is running:

```bash
docker ps
```

You should see `omnisolve-postgres` in the list.

### 3. Run Tests

Execute the test suite to ensure basic functionality.

```bash
mvn test
# or
./mvnw test
```

**Expected Result:** All tests pass.

### 4. Package the Application

Build the executable JAR file.

```bash
mvn package -DskipTests
# or
./mvnw package -DskipTests
```

**Expected Result:** JAR file created in `target/omnisolve-api-0.0.1-SNAPSHOT.jar`

### 5. Start the Application

Run the Spring Boot application.

```bash
mvn spring-boot:run
# or
./mvnw spring-boot:run
```

**Expected Result:** Application starts without exceptions. Look for:

```
Started OmnisolveApiApplication in X.XXX seconds
```

### 6. Verify Database Migrations

Check that Flyway migrations executed successfully.

Connect to the database:

```bash
docker exec -it omnisolve-postgres psql -U omnisolve -d omnisolve
```

List tables:

```sql
\dt
```

**Expected Tables:**
- audit_logs
- clauses
- departments
- document_reviews
- document_statuses
- document_types
- document_versions
- documents
- flyway_schema_history

Check migration history:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

**Expected Migrations:**
- V1__init.sql
- V2__seed_data.sql
- V3__seed_documents.sql

Exit psql:

```sql
\q
```

### 7. Verify Seed Data

Check that seed data was inserted correctly.

```bash
# Document types
docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -c "SELECT * FROM document_types;"

# Document statuses
docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -c "SELECT * FROM document_statuses;"

# Documents
docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -c "SELECT id, title, status_id FROM documents;"
```

**Expected Result:** Each query returns seeded records.

### 8. API Health Check

Test the health endpoint (no authentication required).

```bash
curl http://localhost:8080/api/health
```

**Expected Response:**

```json
{"status":"ok"}
```

### 9. API Smoke Test - Documents Endpoint

Test the documents endpoint (requires authentication).

```bash
curl -i http://localhost:8080/api/documents
```

**Expected Response:** HTTP 401 Unauthorized (because no JWT token was provided)

This confirms the endpoint exists and security is working.

### 10. Swagger UI Verification

Open your browser and navigate to:

```
http://localhost:8080/swagger-ui.html
```

**Expected Result:** Swagger UI loads showing all API endpoints.

### 11. Check Application Logs

Review the application logs for any errors or warnings.

Look for:
- ✓ Successful database connection
- ✓ Flyway migrations completed
- ✓ No startup exceptions
- ✓ No S3 initialization errors (warnings are OK for local dev)

## Verification Checklist

- [ ] Build completes successfully
- [ ] Docker PostgreSQL container starts
- [ ] Tests pass
- [ ] Application JAR is created
- [ ] Application starts without exceptions
- [ ] Database connection successful
- [ ] Flyway migrations executed (V1, V2, V3)
- [ ] All expected tables exist
- [ ] Seed data present in database
- [ ] Health endpoint returns `{"status":"ok"}`
- [ ] Documents endpoint returns 401 (protected)
- [ ] Swagger UI accessible
- [ ] No critical errors in logs

## Troubleshooting

### Build Fails

- Ensure Java 21 is installed: `java -version`
- Check Maven installation: `mvn --version`
- Clear Maven cache: `mvn clean`

### Database Connection Fails

- Verify PostgreSQL is running: `docker ps`
- Check database logs: `docker logs omnisolve-postgres`
- Verify connection settings in `application.yml`

### Migrations Fail

- Check Flyway schema history: `SELECT * FROM flyway_schema_history;`
- Review migration files in `src/main/resources/db/migration/`
- Check for SQL syntax errors in migration files

### Application Won't Start

- Check if port 8080 is already in use
- Review application logs for stack traces
- Verify all environment variables are set correctly

### API Returns Unexpected Errors

- Check application logs
- Verify database has seed data
- Ensure security configuration allows access to health endpoint

## Cleanup

To stop and remove all containers:

```bash
docker compose down -v
```

To stop the Spring Boot application, press `Ctrl+C` in the terminal where it's running.

## Next Steps

After successful verification:

1. Configure AWS credentials for S3 integration
2. Set up AWS Cognito for authentication
3. Deploy to staging environment
4. Run integration tests
5. Configure CI/CD pipeline

## Support

If verification fails, check:
- Application logs
- Docker container logs: `docker logs omnisolve-postgres`
- Database connectivity
- Port availability (5432, 8080)
