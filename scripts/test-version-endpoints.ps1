# Test script for Document Version API endpoints
# Usage: .\scripts\test-version-endpoints.ps1

$baseUrl = "http://localhost:5000"
$documentId = ""  # Replace with actual document ID after creating one

Write-Host "=== Document Version API Test Script ===" -ForegroundColor Cyan
Write-Host ""

# Test 1: Create a test document first
Write-Host "Test 1: Creating a test document..." -ForegroundColor Yellow
$createDocBody = @{
    title = "Test Document for Versions"
    summary = "Testing version upload functionality"
    typeId = 1
    departmentId = 1
    ownerId = "test-user"
    createdBy = "test-user"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/documents" -Method Post -Body $createDocBody -ContentType "application/json"
    $documentId = $response.id
    Write-Host "✓ Document created with ID: $documentId" -ForegroundColor Green
    Write-Host "  Status: $($response.statusName)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to create document: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 2: Get versions (should be empty initially)
Write-Host "Test 2: Getting versions (should be empty)..." -ForegroundColor Yellow
try {
    $versions = Invoke-RestMethod -Uri "$baseUrl/api/documents/$documentId/versions" -Method Get
    if ($versions.Count -eq 0) {
        Write-Host "✓ No versions found (expected)" -ForegroundColor Green
    } else {
        Write-Host "✗ Expected 0 versions, got $($versions.Count)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Failed to get versions: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: Upload a version (create a test PDF file)
Write-Host "Test 3: Uploading a test PDF version..." -ForegroundColor Yellow

# Create a simple test PDF file
$testPdfContent = "%PDF-1.4`n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj xref 0 4 0000000000 65535 f 0000000009 00000 n 0000000056 00000 n 0000000115 00000 n trailer<</Size 4/Root 1 0 R>>startxref 211 %%EOF"
$testPdfPath = "test-document.pdf"
[System.IO.File]::WriteAllText($testPdfPath, $testPdfContent)

try {
    $form = @{
        file = Get-Item -Path $testPdfPath
    }
    $response = Invoke-RestMethod -Uri "$baseUrl/api/documents/$documentId/versions" -Method Post -Form $form
    Write-Host "✓ Version uploaded successfully" -ForegroundColor Green
    Write-Host "  Version Number: $($response.versionNumber)" -ForegroundColor Gray
    Write-Host "  File Name: $($response.fileName)" -ForegroundColor Gray
    Write-Host "  File Size: $($response.fileSize) bytes" -ForegroundColor Gray
    Write-Host "  MIME Type: $($response.mimeType)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to upload version: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "  Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

# Clean up test file
Remove-Item -Path $testPdfPath -ErrorAction SilentlyContinue

Write-Host ""

# Test 4: Get versions again (should have 1 version)
Write-Host "Test 4: Getting versions (should have 1)..." -ForegroundColor Yellow
try {
    $versions = Invoke-RestMethod -Uri "$baseUrl/api/documents/$documentId/versions" -Method Get
    if ($versions.Count -eq 1) {
        Write-Host "✓ Found 1 version (expected)" -ForegroundColor Green
        Write-Host "  Version: $($versions[0].versionNumber)" -ForegroundColor Gray
        Write-Host "  Uploaded By: $($versions[0].uploadedBy)" -ForegroundColor Gray
        Write-Host "  Uploaded At: $($versions[0].uploadedAt)" -ForegroundColor Gray
    } else {
        Write-Host "✗ Expected 1 version, got $($versions.Count)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Failed to get versions: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 5: Try to upload invalid file type (should fail)
Write-Host "Test 5: Testing file type validation (should fail)..." -ForegroundColor Yellow
$testTxtPath = "test-invalid.txt"
"This is a text file" | Out-File -FilePath $testTxtPath

try {
    $form = @{
        file = Get-Item -Path $testTxtPath
    }
    $response = Invoke-RestMethod -Uri "$baseUrl/api/documents/$documentId/versions" -Method Post -Form $form
    Write-Host "✗ Should have rejected text file" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "✓ Correctly rejected invalid file type" -ForegroundColor Green
    } else {
        Write-Host "✗ Unexpected error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Remove-Item -Path $testTxtPath -ErrorAction SilentlyContinue

Write-Host ""

# Test 6: Submit document for approval and try to upload (should fail)
Write-Host "Test 6: Testing status validation..." -ForegroundColor Yellow
try {
    # Submit document
    $response = Invoke-RestMethod -Uri "$baseUrl/api/documents/$documentId/submit" -Method Post
    Write-Host "  Document submitted, status: $($response.statusName)" -ForegroundColor Gray
    
    # Try to upload (should fail)
    $testPdfContent = "%PDF-1.4`n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj xref 0 4 0000000000 65535 f 0000000009 00000 n 0000000056 00000 n 0000000115 00000 n trailer<</Size 4/Root 1 0 R>>startxref 211 %%EOF"
    $testPdfPath = "test-document2.pdf"
    [System.IO.File]::WriteAllText($testPdfPath, $testPdfContent)
    
    $form = @{
        file = Get-Item -Path $testPdfPath
    }
    $response = Invoke-RestMethod -Uri "$baseUrl/api/documents/$documentId/versions" -Method Post -Form $form
    Write-Host "✗ Should have rejected upload for Pending Approval status" -ForegroundColor Red
    Remove-Item -Path $testPdfPath -ErrorAction SilentlyContinue
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "✓ Correctly rejected upload for non-Draft/Active status" -ForegroundColor Green
    } else {
        Write-Host "✗ Unexpected error: $($_.Exception.Message)" -ForegroundColor Red
    }
    Remove-Item -Path $testPdfPath -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan
Write-Host "Document ID for manual testing: $documentId" -ForegroundColor Gray
