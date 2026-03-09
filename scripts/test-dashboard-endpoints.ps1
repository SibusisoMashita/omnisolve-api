# Test the new dashboard endpoints
# Run this after starting the application

$baseUrl = "http://localhost:8080/api/documents"

Write-Host "Testing Dashboard Endpoints" -ForegroundColor Green
Write-Host "============================`n" -ForegroundColor Green

# Test 1: Get document statistics
Write-Host "1. Testing GET /api/documents/stats" -ForegroundColor Yellow
try {
    $stats = Invoke-RestMethod -Uri "$baseUrl/stats" -Method Get
    Write-Host "   ✓ Stats retrieved:" -ForegroundColor Green
    $stats | ConvertTo-Json
} catch {
    Write-Host "   ✗ Failed: $_" -ForegroundColor Red
}

Write-Host "`n"

# Test 2: Get attention items
Write-Host "2. Testing GET /api/documents/attention" -ForegroundColor Yellow
try {
    $attention = Invoke-RestMethod -Uri "$baseUrl/attention" -Method Get
    Write-Host "   ✓ Attention items retrieved:" -ForegroundColor Green
    $attention | ConvertTo-Json -Depth 3
} catch {
    Write-Host "   ✗ Failed: $_" -ForegroundColor Red
}

Write-Host "`n"

# Test 3: Get upcoming reviews (default 30 days)
Write-Host "3. Testing GET /api/documents/reviews/upcoming" -ForegroundColor Yellow
try {
    $upcoming = Invoke-RestMethod -Uri "$baseUrl/reviews/upcoming" -Method Get
    Write-Host "   ✓ Upcoming reviews retrieved:" -ForegroundColor Green
    $upcoming | ConvertTo-Json -Depth 2
} catch {
    Write-Host "   ✗ Failed: $_" -ForegroundColor Red
}

Write-Host "`n"

# Test 4: Get upcoming reviews (custom days)
Write-Host "4. Testing GET /api/documents/reviews/upcoming?days=7" -ForegroundColor Yellow
try {
    $upcoming7 = Invoke-RestMethod -Uri "$baseUrl/reviews/upcoming?days=7" -Method Get
    Write-Host "   ✓ Upcoming reviews (7 days) retrieved:" -ForegroundColor Green
    $upcoming7 | ConvertTo-Json -Depth 2
} catch {
    Write-Host "   ✗ Failed: $_" -ForegroundColor Red
}

Write-Host "`n"

# Test 5: Get workflow statistics
Write-Host "5. Testing GET /api/documents/workflow" -ForegroundColor Yellow
try {
    $workflow = Invoke-RestMethod -Uri "$baseUrl/workflow" -Method Get
    Write-Host "   ✓ Workflow stats retrieved:" -ForegroundColor Green
    $workflow | ConvertTo-Json
} catch {
    Write-Host "   ✗ Failed: $_" -ForegroundColor Red
}

Write-Host "`n"
Write-Host "============================`n" -ForegroundColor Green
Write-Host "Testing Complete!" -ForegroundColor Green

