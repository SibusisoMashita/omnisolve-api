# API Documentation

## Base URL

- **Local**: `http://localhost:5000`
- **DEV**: `http://<dev-environment>.elasticbeanstalk.com`
- **PROD**: `http://<prod-environment>.elasticbeanstalk.com`

## Authentication

All `/api/**` endpoints require JWT authentication via AWS Cognito when `JWT_ENABLED=true`.

Public endpoints:
- `/actuator/health`
- `/health`
- `/swagger-ui/**`
- `/v3/api-docs/**`

### Authentication Header

```http
Authorization: Bearer <jwt-token>
```

### Obtaining a Token

1. Authenticate with AWS Cognito using your credentials
2. Receive a JWT token from Cognito
3. Include the token in the `Authorization` header for all API requests

### Local Development

JWT authentication can be disabled for local development by setting:

```yaml
app:
  security:
    jwt:
      enabled: false
```

## Interactive API Documentation

Swagger UI is available at:

```
http://localhost:5000/swagger-ui.html
```

OpenAPI specification:

```
http://localhost:5000/v3/api-docs
```

## API Endpoints

### Health Check

#### GET /api/health

Health check endpoint (requires authentication when `JWT_ENABLED=true` because it is under `/api/**`).

**Response:**
```json
{
  "status": "ok"
}
```

---

## Documents API

Manage controlled documents with workflow states and version control.

### List Documents

#### GET /api/documents

Retrieve all documents.

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "documentNumber": "DOC-001",
    "title": "Quality Management System",
    "summary": "Overview of QMS procedures",
    "typeId": 1,
    "typeName": "Policy",
    "departmentId": 1,
    "departmentName": "Quality",
    "statusId": 3,
    "statusName": "Active",
    "ownerId": "user123",
    "nextReviewAt": "2024-12-31T00:00:00Z",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-20T14:45:00Z"
  }
]
```

### Get Document by ID

#### GET /api/documents/{id}

Retrieve a specific document by UUID.

**Parameters:**
- `id` (path, UUID) - Document UUID

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "documentNumber": "DOC-001",
  "title": "Quality Management System",
  "summary": "Overview of QMS procedures",
  "typeId": 1,
  "typeName": "Policy",
  "departmentId": 1,
  "departmentName": "Quality",
  "statusId": 3,
  "statusName": "Active",
  "ownerId": "user123",
  "nextReviewAt": "2024-12-31T00:00:00Z",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-20T14:45:00Z"
}
```

### Create Document

#### POST /api/documents

Create a new document in Draft status.

**Request Body:**
```json
{
  "documentNumber": "DOC-002",
  "title": "Safety Procedures",
  "summary": "Workplace safety guidelines",
  "typeId": 2,
  "departmentId": 3,
  "ownerId": "user456",
  "nextReviewAt": "2024-12-31T00:00:00Z",
  "clauseIds": [1, 2, 3]
}
```

**Response:** `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "documentNumber": "DOC-002",
  "title": "Safety Procedures",
  "statusName": "Draft",
  ...
}
```

### Update Document

#### PUT /api/documents/{id}

Update an existing document (only allowed in Draft status).

**Parameters:**
- `id` (path, UUID) - Document UUID

**Request Body:**
```json
{
  "title": "Updated Safety Procedures",
  "summary": "Updated workplace safety guidelines",
  "typeId": 2,
  "departmentId": 3,
  "ownerId": "user456",
  "nextReviewAt": "2024-12-31T00:00:00Z",
  "clauseIds": [1, 2, 3, 4]
}
```

**Response:** `200 OK`

### Submit Document for Approval

#### POST /api/documents/{id}/submit

Submit a Draft document for approval (transitions to Pending status).

**Parameters:**
- `id` (path, UUID) - Document UUID

**Response:** `200 OK`

### Approve Document

#### POST /api/documents/{id}/approve

Approve a Pending document (transitions to Active status).

**Parameters:**
- `id` (path, UUID) - Document UUID

**Response:** `200 OK`

### Reject Document

#### POST /api/documents/{id}/reject

Reject a Pending document (transitions back to Draft status).

**Parameters:**
- `id` (path, UUID) - Document UUID

**Response:** `200 OK`

### Archive Document

#### POST /api/documents/{id}/archive

Archive an Active document (transitions to Archived status).

**Parameters:**
- `id` (path, UUID) - Document UUID

**Response:** `200 OK`

### Upload Document Version

#### POST /api/documents/{id}/versions

Upload a new version file for a document (stored in S3).

**Parameters:**
- `id` (path, UUID) - Document UUID

**Request:**
- Content-Type: `multipart/form-data`
- Form field: `file` (binary file)

**Response:** `201 Created`
```json
{
  "id": 1,
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "versionNumber": 2,
  "fileName": "qms-v2.pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "s3Key": "documents/550e8400-e29b-41d4-a716-446655440000/v2/qms-v2.pdf",
  "uploadedBy": "user123",
  "uploadedAt": "2024-01-20T14:45:00Z"
}
```

---

## Clauses API

Manage ISO clauses that can be linked to documents.

### List Clauses

#### GET /api/clauses

Retrieve all clauses.

**Response:**
```json
[
  {
    "id": 1,
    "code": "ISO-9001-4.1",
    "title": "Understanding the organization and its context",
    "description": "The organization shall determine external and internal issues..."
  }
]
```

### Create Clause

#### POST /api/clauses

Create a new clause.

**Request Body:**
```json
{
  "code": "ISO-9001-4.2",
  "title": "Understanding the needs and expectations of interested parties",
  "description": "The organization shall determine interested parties..."
}
```

**Response:** `201 Created`

### Update Clause

#### PUT /api/clauses/{id}

Update an existing clause.

**Parameters:**
- `id` (path, Long) - Clause ID

**Request Body:**
```json
{
  "code": "ISO-9001-4.2",
  "title": "Updated title",
  "description": "Updated description"
}
```

**Response:** `200 OK`

---

## Departments API

Manage organizational departments.

### List Departments

#### GET /api/departments

Retrieve all departments.

**Response:**
```json
[
  {
    "id": 1,
    "name": "Quality",
    "description": "Quality Assurance and Control"
  }
]
```

### Create Department

#### POST /api/departments

Create a new department.

**Request Body:**
```json
{
  "name": "Engineering",
  "description": "Engineering and Development"
}
```

**Response:** `201 Created`

### Update Department

#### PUT /api/departments/{id}

Update an existing department.

**Parameters:**
- `id` (path, Long) - Department ID

**Request Body:**
```json
{
  "name": "Engineering",
  "description": "Updated description"
}
```

**Response:** `200 OK`

---

## Document Types API

Manage document type categories.

### List Document Types

#### GET /api/document-types

Retrieve all document types.

**Response:**
```json
[
  {
    "id": 1,
    "name": "Policy",
    "description": "High-level organizational policies"
  }
]
```

### Create Document Type

#### POST /api/document-types

Create a new document type.

**Request Body:**
```json
{
  "name": "Procedure",
  "description": "Step-by-step procedures"
}
```

**Response:** `201 Created`

### Update Document Type

#### PUT /api/document-types/{id}

Update an existing document type.

**Parameters:**
- `id` (path, Long) - Document Type ID

**Request Body:**
```json
{
  "name": "Procedure",
  "description": "Updated description"
}
```

**Response:** `200 OK`

---

## Document Workflow States

Documents follow a state machine workflow:

```
Draft → Pending → Active → Archived
         ↓
       Draft (rejected)
```

| Status | Description | Allowed Operations |
|--------|-------------|-------------------|
| **Draft** | Initial state, editable | Update, Submit |
| **Pending** | Awaiting approval | Approve, Reject |
| **Active** | Approved and published | Archive, Upload Version |
| **Archived** | Retired document | None |

## Error Responses

### Standard Error Format

```json
{
  "timestamp": "2024-01-20T14:45:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid document status transition",
  "path": "/api/documents/550e8400-e29b-41d4-a716-446655440000/submit"
}
```

### Common HTTP Status Codes

- `200 OK` - Request succeeded
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data or business rule violation
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Rate Limiting

Currently, no rate limiting is implemented. Consider implementing rate limiting for production use.

## Versioning

API version is included in the base path: `/api/`

Future versions may use: `/api/v2/`, `/api/v3/`, etc.

## CORS

CORS configuration should be set up based on your frontend domain requirements.

## Frontend Integration

The API is designed to work with a frontend application running on `http://localhost:5173` (Vite dev server).

### Frontend Route Mapping

| Frontend Route | API Endpoint | Description |
|---------------|--------------|-------------|
| `/documents` | `GET /api/documents` | List all documents |
| `/documents/:id` | `GET /api/documents/:id` | View document details |

**Note:** The frontend uses `/documents` (not `/documents/all`) for the document list view.

## Best Practices

1. **Always include JWT token** in the Authorization header (except for health check)
2. **Use UUIDs** for document IDs (not sequential integers)
3. **Follow workflow rules** - documents can only transition through valid states
4. **Upload files after document creation** - create document first, then upload versions
5. **Link clauses during creation** - include `clauseIds` array in document creation request
6. **Check file size limits** - ensure uploaded files are within acceptable size limits
7. **Use appropriate MIME types** - ensure file uploads have correct content types

## Example: Complete Document Creation Flow

```bash
# 1. Create document
curl -X POST http://localhost:5000/api/documents \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "documentNumber": "DOC-003",
    "title": "New Policy",
    "typeId": 1,
    "departmentId": 1,
    "ownerId": "user123",
    "clauseIds": [1, 2]
  }'

# Response: { "id": "770e8400-...", "statusName": "Draft", ... }

# 2. Upload file version
curl -X POST http://localhost:5000/api/documents/770e8400-.../versions \
  -H "Authorization: Bearer <token>" \
  -F "file=@policy.pdf"

# 3. Submit for approval
curl -X POST http://localhost:5000/api/documents/770e8400-.../submit \
  -H "Authorization: Bearer <token>"

# 4. Approve document
curl -X POST http://localhost:5000/api/documents/770e8400-.../approve \
  -H "Authorization: Bearer <token>"
```
