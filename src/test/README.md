# OmniSolve Integration Tests

This directory contains integration tests for the OmniSolve backend API.

## Overview

Integration tests verify the complete application stack:
- Spring Boot application startup
- PostgreSQL database (via Testcontainers)
- Flyway migrations
- HTTP endpoints
- Request/response serialization
- Database persistence

## Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=HealthControllerIT

# Run with verbose output
./mvnw test -X
```

## Test Structure

```
src/test/java/com/omnisolve/
├── IntegrationTestBase.java          # Base class for all integration tests
└── controller/
    ├── HealthControllerIT.java       # Health endpoint tests
    ├── DocumentControllerIT.java     # Document API tests
    ├── DocumentTypeControllerIT.java # Document type API tests
    ├── DepartmentControllerIT.java   # Department API tests
    ├── ClauseControllerIT.java       # Clause API tests
    └── OpenApiIT.java                # OpenAPI/Swagger tests
```

## Requirements

- Docker must be running (for Testcontainers)
- Java 21
- Maven 3.6+

## Test Configuration

Tests use the `test` profile with configuration in `src/test/resources/application-test.yml`:
- JWT security disabled
- AWS S3 disabled
- PostgreSQL provided by Testcontainers
- Flyway migrations enabled

## Container Reuse

Testcontainers is configured to reuse containers across test runs for faster execution.
The PostgreSQL container stays running between test executions, reducing startup time from ~10s to <1s.

To enable container reuse globally, create `~/.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

## Test Isolation

Tests use `@Transactional` annotation to automatically roll back changes after each test,
ensuring test isolation without manual cleanup.

## Troubleshooting

### Docker not available
Ensure Docker Desktop is running. Testcontainers requires the Docker daemon.

### Port conflicts
Tests use `RANDOM_PORT` for the web environment, so port conflicts should not occur.

### Slow tests
- Enable container reuse (see above)
- Check Docker resource allocation
- Review database query performance

### Migration failures
- Check Flyway scripts in `src/main/resources/db/migration/`
- Verify PostgreSQL version compatibility (tests use postgres:16-alpine)
- Review migration logs in test output
