# OmniSolve Backend Verification Script (PowerShell)
# This script performs automated verification of the entire system

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "OmniSolve Backend Verification" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$results = @()

function Print-Status {
    param($success, $message)
    if ($success) {
        Write-Host "✓ $message" -ForegroundColor Green
        $script:results += "✓ $message"
    } else {
        Write-Host "✗ $message" -ForegroundColor Red
        $script:results += "✗ $message"
    }
}

# 1. Check Maven Installation
Write-Host "1. Checking Maven installation..." -ForegroundColor Yellow
$mvnCmd = "mvn"
try {
    $mvnVersion = & mvn --version 2>&1
    Write-Host $mvnVersion
    Print-Status $true "Maven is installed"
} catch {
    Write-Host "Maven not found. Checking for Maven wrapper..." -ForegroundColor Yellow
    if (Test-Path "mvnw.cmd") {
        $mvnCmd = ".\mvnw.cmd"
        Print-Status $true "Maven wrapper found"
    } else {
        Write-Host "Installing Maven wrapper..." -ForegroundColor Yellow
        # Download Maven wrapper files
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd" -OutFile "mvnw.cmd"
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw" -OutFile "mvnw"
        New-Item -ItemType Directory -Force -Path ".mvn\wrapper" | Out-Null
        Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -OutFile ".mvn\wrapper\maven-wrapper.jar"
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.properties" -OutFile ".mvn\wrapper\maven-wrapper.properties"
        $mvnCmd = ".\mvnw.cmd"
        Print-Status $true "Maven wrapper installed"
    }
}
Write-Host ""

# 2. Build Verification
Write-Host "2. Building project..." -ForegroundColor Yellow
try {
    & $mvnCmd clean compile -DskipTests
    if ($LASTEXITCODE -eq 0) {
        Print-Status $true "Build successful"
    } else {
        Print-Status $false "Build failed"
        exit 1
    }
} catch {
    Print-Status $false "Build failed with exception: $_"
    exit 1
}
Write-Host ""

# 3. Docker Environment Verification
Write-Host "3. Starting Docker environment..." -ForegroundColor Yellow
try {
    docker compose down -v 2>$null
    docker compose up -d postgres
    if ($LASTEXITCODE -eq 0) {
        Print-Status $true "PostgreSQL container started"
    } else {
        Print-Status $false "Failed to start PostgreSQL"
        exit 1
    }
} catch {
    Print-Status $false "Docker compose failed: $_"
    exit 1
}

# Wait for PostgreSQL to be ready
Write-Host "Waiting for PostgreSQL to be ready..."
$ready = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        docker exec omnisolve-postgres pg_isready -U omnisolve -d omnisolve 2>$null
        if ($LASTEXITCODE -eq 0) {
            Print-Status $true "PostgreSQL is ready"
            $ready = $true
            break
        }
    } catch {}
    Start-Sleep -Seconds 1
}
if (-not $ready) {
    Print-Status $false "PostgreSQL failed to start"
    exit 1
}
Write-Host ""

# 4. Run tests
Write-Host "4. Running tests..." -ForegroundColor Yellow
try {
    & $mvnCmd test
    if ($LASTEXITCODE -eq 0) {
        Print-Status $true "Tests passed"
    } else {
        Print-Status $false "Tests failed"
    }
} catch {
    Print-Status $false "Tests failed with exception: $_"
}
Write-Host ""

# 5. Package application
Write-Host "5. Packaging application..." -ForegroundColor Yellow
try {
    & $mvnCmd package -DskipTests
    if ($LASTEXITCODE -eq 0) {
        Print-Status $true "Application packaged"
    } else {
        Print-Status $false "Packaging failed"
        exit 1
    }
} catch {
    Print-Status $false "Packaging failed: $_"
    exit 1
}
Write-Host ""

# 6. Start application in background
Write-Host "6. Starting Spring Boot application..." -ForegroundColor Yellow
$appJob = Start-Job -ScriptBlock {
    param($mvnCmd)
    Set-Location $using:PWD
    & $mvnCmd spring-boot:run -DskipTests
} -ArgumentList $mvnCmd

Write-Host "Application job started: $($appJob.Id)"

# Wait for application to start
Write-Host "Waiting for application to start..."
$started = $false
for ($i = 1; $i -le 60; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Print-Status $true "Application started successfully"
            $started = $true
            break
        }
    } catch {}
    Start-Sleep -Seconds 2
}
if (-not $started) {
    Print-Status $false "Application failed to start"
    Stop-Job $appJob
    Remove-Job $appJob
    docker compose down
    exit 1
}
Write-Host ""

# 7. Migration Validation
Write-Host "7. Validating database migrations..." -ForegroundColor Yellow
$expectedTables = @("audit_logs", "clauses", "departments", "document_reviews", "document_statuses", "document_types", "document_versions", "documents", "flyway_schema_history")

foreach ($table in $expectedTables) {
    try {
        $result = docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM $table;" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Print-Status $true "Table exists: $table"
        } else {
            Print-Status $false "Table missing: $table"
        }
    } catch {
        Print-Status $false "Table missing: $table"
    }
}
Write-Host ""

# 8. Seed Data Validation
Write-Host "8. Validating seed data..." -ForegroundColor Yellow

try {
    $docTypes = docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM document_types;"
    $docTypes = $docTypes.Trim()
    if ([int]$docTypes -gt 0) {
        Print-Status $true "Document types seeded ($docTypes records)"
    } else {
        Print-Status $false "No document types found"
    }
} catch {
    Print-Status $false "Failed to query document types"
}

try {
    $docStatuses = docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM document_statuses;"
    $docStatuses = $docStatuses.Trim()
    if ([int]$docStatuses -gt 0) {
        Print-Status $true "Document statuses seeded ($docStatuses records)"
    } else {
        Print-Status $false "No document statuses found"
    }
} catch {
    Print-Status $false "Failed to query document statuses"
}

try {
    $documents = docker exec omnisolve-postgres psql -U omnisolve -d omnisolve -t -c "SELECT COUNT(*) FROM documents;"
    $documents = $documents.Trim()
    if ([int]$documents -gt 0) {
        Print-Status $true "Documents seeded ($documents records)"
    } else {
        Print-Status $false "No documents found"
    }
} catch {
    Print-Status $false "Failed to query documents"
}
Write-Host ""

# 9. API Health Check
Write-Host "9. Testing API health endpoint..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get
    if ($healthResponse.status -eq "ok") {
        Print-Status $true "Health endpoint returns OK"
        Write-Host "Response: $($healthResponse | ConvertTo-Json)"
    } else {
        Print-Status $false "Health endpoint failed"
        Write-Host "Response: $($healthResponse | ConvertTo-Json)"
    }
} catch {
    Print-Status $false "Health endpoint failed: $_"
}
Write-Host ""

# 10. API Smoke Test
Write-Host "10. Testing documents API endpoint..." -ForegroundColor Yellow
try {
    $docsResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/documents" -Method Get -UseBasicParsing -ErrorAction SilentlyContinue
    if ($docsResponse.StatusCode -eq 200) {
        Print-Status $true "Documents endpoint returns 200"
    } else {
        Print-Status $false "Documents endpoint returned unexpected status: $($docsResponse.StatusCode)"
    }
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Print-Status $true "Documents endpoint is protected (401 Unauthorized as expected)"
    } else {
        Print-Status $false "Documents endpoint error: $_"
    }
}
Write-Host ""

# Cleanup
Write-Host "Cleaning up..." -ForegroundColor Yellow
Stop-Job $appJob -ErrorAction SilentlyContinue
Remove-Job $appJob -ErrorAction SilentlyContinue
docker compose down

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
foreach ($result in $results) {
    if ($result.StartsWith("✓")) {
        Write-Host $result -ForegroundColor Green
    } else {
        Write-Host $result -ForegroundColor Red
    }
}
Write-Host ""

$successCount = ($results | Where-Object { $_.StartsWith("✓") }).Count
$totalCount = $results.Count

Write-Host "Passed: $successCount/$totalCount checks" -ForegroundColor Cyan

if ($successCount -eq $totalCount) {
    Write-Host "All verification checks passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Some verification checks failed." -ForegroundColor Red
    exit 1
}
