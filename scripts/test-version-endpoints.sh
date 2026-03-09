#!/bin/bash
# Test script for Document Version API endpoints
# Usage: ./scripts/test-version-endpoints.sh

BASE_URL="http://localhost:5000"
DOCUMENT_ID=""

echo "=== Document Version API Test Script ==="
echo ""

# Test 1: Create a test document first
echo "Test 1: Creating a test document..."
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Document for Versions",
    "summary": "Testing version upload functionality",
    "typeId": 1,
    "departmentId": 1,
    "ownerId": "test-user",
    "createdBy": "test-user"
  }')

DOCUMENT_ID=$(echo $CREATE_RESPONSE | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ -n "$DOCUMENT_ID" ]; then
    echo "✓ Document created with ID: $DOCUMENT_ID"
    STATUS=$(echo $CREATE_RESPONSE | grep -o '"statusName":"[^"]*' | cut -d'"' -f4)
    echo "  Status: $STATUS"
else
    echo "✗ Failed to create document"
    exit 1
fi

echo ""

# Test 2: Get versions (should be empty initially)
echo "Test 2: Getting versions (should be empty)..."
VERSIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/$DOCUMENT_ID/versions")
VERSION_COUNT=$(echo $VERSIONS_RESPONSE | grep -o '"id"' | wc -l)

if [ "$VERSION_COUNT" -eq 0 ]; then
    echo "✓ No versions found (expected)"
else
    echo "✗ Expected 0 versions, got $VERSION_COUNT"
fi

echo ""

# Test 3: Upload a version (create a test PDF file)
echo "Test 3: Uploading a test PDF version..."

# Create a simple test PDF file
cat > test-document.pdf << 'EOF'
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj xref 0 4 0000000000 65535 f 0000000009 00000 n 0000000056 00000 n 0000000115 00000 n trailer<</Size 4/Root 1 0 R>>startxref 211 %%EOF
EOF

UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/$DOCUMENT_ID/versions" \
  -F "file=@test-document.pdf")

if echo "$UPLOAD_RESPONSE" | grep -q '"versionNumber"'; then
    echo "✓ Version uploaded successfully"
    VERSION_NUM=$(echo $UPLOAD_RESPONSE | grep -o '"versionNumber":[0-9]*' | cut -d':' -f2)
    FILE_NAME=$(echo $UPLOAD_RESPONSE | grep -o '"fileName":"[^"]*' | cut -d'"' -f4)
    echo "  Version Number: $VERSION_NUM"
    echo "  File Name: $FILE_NAME"
else
    echo "✗ Failed to upload version"
    echo "  Response: $UPLOAD_RESPONSE"
fi

rm -f test-document.pdf

echo ""

# Test 4: Get versions again (should have 1 version)
echo "Test 4: Getting versions (should have 1)..."
VERSIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/documents/$DOCUMENT_ID/versions")
VERSION_COUNT=$(echo $VERSIONS_RESPONSE | grep -o '"id"' | wc -l)

if [ "$VERSION_COUNT" -eq 1 ]; then
    echo "✓ Found 1 version (expected)"
    VERSION_NUM=$(echo $VERSIONS_RESPONSE | grep -o '"versionNumber":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  Version: $VERSION_NUM"
else
    echo "✗ Expected 1 version, got $VERSION_COUNT"
fi

echo ""

# Test 5: Try to upload invalid file type (should fail)
echo "Test 5: Testing file type validation (should fail)..."
echo "This is a text file" > test-invalid.txt

UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/documents/$DOCUMENT_ID/versions" \
  -F "file=@test-invalid.txt")

HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ Correctly rejected invalid file type"
else
    echo "✗ Expected 400 status code, got $HTTP_CODE"
fi

rm -f test-invalid.txt

echo ""

# Test 6: Submit document for approval and try to upload (should fail)
echo "Test 6: Testing status validation..."

# Submit document
SUBMIT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/$DOCUMENT_ID/submit")
STATUS=$(echo $SUBMIT_RESPONSE | grep -o '"statusName":"[^"]*' | cut -d'"' -f4)
echo "  Document submitted, status: $STATUS"

# Try to upload (should fail)
cat > test-document2.pdf << 'EOF'
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj xref 0 4 0000000000 65535 f 0000000009 00000 n 0000000056 00000 n 0000000115 00000 n trailer<</Size 4/Root 1 0 R>>startxref 211 %%EOF
EOF

UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/documents/$DOCUMENT_ID/versions" \
  -F "file=@test-document2.pdf")

HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ Correctly rejected upload for non-Draft/Active status"
else
    echo "✗ Expected 400 status code, got $HTTP_CODE"
fi

rm -f test-document2.pdf

echo ""
echo "=== Test Complete ==="
echo "Document ID for manual testing: $DOCUMENT_ID"
