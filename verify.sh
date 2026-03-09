#!/bin/bash

# OmniSolve Backend Verification Script
# This script performs automated verification of the entire system

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "OmniSolve Backend Verification"
echo "=========================================="
echo ""

# Check results array
declare -a RESULTS

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
        RESULTS+=("✓ $2")
    else
        echo -e "${RED}✗${NC} $2"
        RESULTS+=("✗ $2")
    fi
}

# 1. Check Maven Installation
echo "1. Checking Maven installation..."
if command -v mvn &> /dev/null; then
    mvn --version
    print_status 0 "Maven is installed"
else
    echo -e "${YELLOW}Maven not found. Installing Maven wrapper...${NC}"
    # Download Maven wrapper
    curl -o mvnw https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw
    curl -o mvnw.cmd https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd
    mkdir -p .mvn/wrapper
    curl -o .mvn/wrapper/maven-wrapper.jar https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    curl -o .mvn/wrapper/maven-wrapper.properties https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.properties
    chmod +x mvnw
    print_status 0 "Maven wrapper installed"
    MVN_CMD="./mvnw"
fi

if [ -z "$MVN_CMD" ]; then
    MVN_CMD="mvn"
fi

echo ""

# 2. Build Verification
echo "2. Building project..."
if $MVN_CMD clean compile -DskipTests; then
    print_status 0 "Build successful"
else
    print_status 1 "Build failed"
    exit 1
fi
echo ""

# 3. Docker Environment Verification
echo "3. Starting Docker environment..."
docker compose down -v 2>/dev/null || true
if docker compose up -d postgres; then
    print_status 0 "PostgreSQL container started"
else
    print_status 1 "Failed to start PostgreSQL"
    exit 1
fi

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec omnisolve-postgres pg_isready -U omnisolve -d omnisolve &>/dev/null; then
        print_status 0 "PostgreSQL is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        print_status 1 "PostgreSQL failed to start"
        exit 1
    fi
    sleep 1
done
echo ""

# 4. Run tests
echo "4. Running tests..."
if $MVN_CMD test; then
    print_status 0 "Tests passed"
else
    print_status 1 "Tests failed"
fi
echo ""

# 5. Package application
echo "5. Packaging application..."
if $MVN_CMD package -DskipTests; then
    print_status 0 "Application packaged"
else
    print_status 1 "Packaging failed"
    exit 1
fi
echo ""

# 6. Start application in background
echo "6. Starting Spring Boot application..."
$MVN_CMD spring-boot:run -DskipTests &
APP_PID=$!
echo "Application PID: $APP_PID"

# Wait for application to start
echo "Waiting for application to start..."
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health &>/dev/null; then
        print_status 0 "Application started successfully"
        break
    fi
    if [ $i -eq 60 ]; then
        print_status 1 "Application failed to start"
        kill $APP_PID 2>/dev/null || true
        docker compose down
        exit 1
    fi
    sleep 2
done
echo ""

# 7. Migration Validation
echo "7. Validating database migrations..."
TABLES=$(docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;")
EXPECTED_TABLES=("audit_logs" "clauses" "departments" "document_reviews" "document_statuses" "document_types" "document_versions" "documents" "flyway_schema_history")

for table in "${EXPECTED_TABLES[@]}"; do
    if echo "$TABLES" | grep -q "$table"; then
        print_status 0 "Table exists: $table"
    else
        print_status 1 "Table missing: $table"
    fi
done
echo ""

# 8. Seed Data Validation
echo "8. Validating seed data..."

DOC_TYPES=$(docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM document_types;")
if [ "$DOC_TYPES" -gt 0 ]; then
    print_status 0 "Document types seeded ($DOC_TYPES records)"
else
    print_status 1 "No document types found"
fi

DOC_STATUSES=$(docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM document_statuses;")
if [ "$DOC_STATUSES" -gt 0 ]; then
    print_status 0 "Document statuses seeded ($DOC_STATUSES records)"
else
    print_status 1 "No document statuses found"
fi

DOCUMENTS=$(docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM documents;")
if [ "$DOCUMENTS" -gt 0 ]; then
    print_status 0 "Documents seeded ($DOCUMENTS records)"
else
    print_status 1 "No documents found"
fi
echo ""

# 9. API Health Check
echo "9. Testing API health endpoint..."
HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/health)
if echo "$HEALTH_RESPONSE" | grep -q '"status":"ok"'; then
    print_status 0 "Health endpoint returns OK"
    echo "Response: $HEALTH_RESPONSE"
else
    print_status 1 "Health endpoint failed"
    echo "Response: $HEALTH_RESPONSE"
fi
echo ""

# 10. API Smoke Test
echo "10. Testing documents API endpoint..."
# Note: This endpoint requires authentication, so we expect 401
DOCS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/documents)
if [ "$DOCS_STATUS" -eq 401 ]; then
    print_status 0 "Documents endpoint is protected (401 Unauthorized as expected)"
elif [ "$DOCS_STATUS" -eq 200 ]; then
    print_status 0 "Documents endpoint returns 200"
else
    print_status 1 "Documents endpoint returned unexpected status: $DOCS_STATUS"
fi
echo ""

# 11. Check application logs for errors
echo "11. Checking application logs for errors..."
sleep 2
if ! grep -i "exception\|error\|failed" target/spring-boot.log 2>/dev/null | grep -v "DEBUG" | head -5; then
    print_status 0 "No critical errors in logs"
else
    print_status 1 "Errors found in application logs"
fi
echo ""

# Cleanup
echo "Cleaning up..."
kill $APP_PID 2>/dev/null || true
docker compose down

echo ""
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
for result in "${RESULTS[@]}"; do
    echo "$result"
done
echo ""

# Count successes
SUCCESS_COUNT=$(printf '%s\n' "${RESULTS[@]}" | grep -c "✓" || true)
TOTAL_COUNT=${#RESULTS[@]}

echo "Passed: $SUCCESS_COUNT/$TOTAL_COUNT checks"

if [ "$SUCCESS_COUNT" -eq "$TOTAL_COUNT" ]; then
    echo -e "${GREEN}All verification checks passed!${NC}"
    exit 0
else
    echo -e "${RED}Some verification checks failed.${NC}"
    exit 1
fi
