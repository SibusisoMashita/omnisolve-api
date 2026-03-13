# OmniSolve API Endpoints

Complete API reference for the OmniSolve backend system. All endpoints require JWT authentication via AWS Cognito unless otherwise noted.

## Base URL

```
Production: https://api.omnisolve.com
Development: http://localhost:8080
```

## Authentication

All endpoints (except `/api/health`) require a valid JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

The JWT token is obtained from AWS Cognito after successful authentication. The token contains the user's `sub` (subject) which is used to identify the authenticated user.

## Common Response Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request payload or parameters
- `401 Unauthorized` - Missing or invalid authentication token
- `403 Forbidden` - User lacks permission for the requested operation
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Pagination

Endpoints that return lists support pagination using Spring Data's `Pageable` interface:

**Query Parameters:**
- `page` - Page number (0-indexed, default: 0)
- `size` - Page size (default: 20)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

**Response Format:**
```json
{
  "content": [...],
  "pageable": {...},
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```


---

## Health Check

### GET /api/health

Health check endpoint (no authentication required).

**Response:** `200 OK`
```json
{
  "status": "UP"
}
```

---

## Clauses

ISO compliance clause management endpoints.

### GET /api/clauses

List all ISO clauses.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "standardId": 1,
    "standardCode": "ISO-9001",
    "standardName": "ISO 9001:2015",
    "code": "4",
    "title": "Context of the Organization",
    "description": "Understanding organizational context and stakeholder needs",
    "parentCode": null,
    "level": 1,
    "sortOrder": 1,
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

### GET /api/clauses/{id}

Get a specific clause by ID.

**Path Parameters:**
- `id` (Long) - Clause ID

**Response:** `200 OK` (same structure as list item above)

### POST /api/clauses

Create a new clause.

**Request Body:**
```json
{
  "standardId": 1,
  "code": "4.1",
  "title": "Understanding the organization and its context",
  "description": "Determine external and internal issues relevant to purpose",
  "parentCode": "4",
  "level": 2,
  "sortOrder": 1
}
```

**Field Descriptions:**
- `standardId` (Long, required) - ID of the ISO standard (1=ISO 9001, 2=ISO 14001, 3=ISO 45001)
- `code` (String, required) - Clause code (e.g., "4.1", "6.2.1")
- `title` (String, required) - Clause title
- `description` (String, optional) - Detailed description
- `parentCode` (String, optional) - Parent clause code for hierarchical structure
- `level` (Integer, optional) - Hierarchy level (default: 1)
- `sortOrder` (Integer, optional) - Display order (default: 0)

**Response:** `201 Created` (same structure as GET response)

### PUT /api/clauses/{id}

Update an existing clause.

**Path Parameters:**
- `id` (Long) - Clause ID

**Request Body:** Same as POST

**Response:** `200 OK`

### DELETE /api/clauses/{id}

Delete a clause.

**Path Parameters:**
- `id` (Long) - Clause ID

**Response:** `204 No Content`


---

## Departments

Department management endpoints.

### GET /api/departments

List all departments.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Quality",
    "description": "Quality management and assurance"
  }
]
```

### GET /api/departments/{id}

Get a specific department.

**Path Parameters:**
- `id` (Long) - Department ID

**Response:** `200 OK`

### POST /api/departments

Create a new department.

**Request Body:**
```json
{
  "name": "Quality",
  "description": "Quality management and assurance"
}
```

**Response:** `201 Created`

### PUT /api/departments/{id}

Update a department.

**Path Parameters:**
- `id` (Long) - Department ID

**Request Body:** Same as POST

**Response:** `200 OK`

### DELETE /api/departments/{id}

Delete a department.

**Path Parameters:**
- `id` (Long) - Department ID

**Response:** `204 No Content`


---

## Document Types

Document type catalog management.

### GET /api/document-types

List all document types.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Policy",
    "description": "High-level organizational policies",
    "requiresClauses": true
  }
]
```

### GET /api/document-types/{id}

Get a specific document type.

**Path Parameters:**
- `id` (Long) - Document type ID

**Response:** `200 OK`

### POST /api/document-types

Create a new document type.

**Request Body:**
```json
{
  "name": "Policy",
  "description": "High-level organizational policies",
  "requiresClauses": true
}
```

**Field Descriptions:**
- `name` (String, required) - Document type name
- `description` (String, optional) - Description of the document type
- `requiresClauses` (Boolean, optional) - Whether documents of this type must be linked to ISO clauses

**Response:** `201 Created`

### PUT /api/document-types/{id}

Update a document type.

**Path Parameters:**
- `id` (Long) - Document type ID

**Request Body:** Same as POST

**Response:** `200 OK`

### DELETE /api/document-types/{id}

Delete a document type.

**Path Parameters:**
- `id` (Long) - Document type ID

**Response:** `204 No Content`


---

## Documents

Document control with workflow, versioning, and S3 storage.

### GET /api/documents

List all documents.

**Response:** `200 OK`
```json
[
  {
    "id": "2b15d006-97f3-4695-9db1-f9fb3e16f8e9",
    "documentNumber": "DOC-2024-001",
    "title": "Quality Management Policy",
    "summary": "Defines quality commitments",
    "typeId": 1,
    "typeName": "Policy",
    "typeRequiresClauses": true,
    "departmentId": 1,
    "departmentName": "Quality",
    "statusId": 2,
    "statusName": "Active",
    "ownerId": "user-123",
    "createdBy": "John Doe",
    "nextReviewAt": "2027-03-09T00:00:00Z",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

### GET /api/documents/{id}

Get a specific document by UUID.

**Path Parameters:**
- `id` (UUID) - Document UUID

**Response:** `200 OK`

### POST /api/documents

Create a new document.

**Request Body:**
```json
{
  "title": "Quality Management Policy",
  "summary": "Defines quality commitments",
  "typeId": 1,
  "departmentId": 1,
  "ownerId": "user-123",
  "createdBy": "John Doe",
  "nextReviewAt": "2027-03-09T00:00:00Z",
  "clauseIds": [1, 2, 3]
}
```

**Field Descriptions:**
- `title` (String, required) - Document title
- `summary` (String, optional) - Document summary or description
- `typeId` (Long, required) - Document type ID
- `departmentId` (Long, required) - Department ID
- `ownerId` (String, required) - Owner user ID (Cognito sub)
- `createdBy` (String, optional) - Creator full name
- `nextReviewAt` (OffsetDateTime, optional) - Next review date
- `clauseIds` (List<Long>, optional) - List of clause IDs to link

**Response:** `201 Created`

### PUT /api/documents/{id}

Update a document.

**Path Parameters:**
- `id` (UUID) - Document UUID

**Request Body:** Same as POST

**Response:** `200 OK`


### GET /api/documents/stats

Get document statistics for dashboard.

**Response:** `200 OK`
```json
{
  "total": 150,
  "active": 120,
  "pending": 10,
  "reviewDue": 5,
  "draft": 10,
  "archived": 5
}
```

### GET /api/documents/attention

Get documents requiring attention (overdue reviews and pending approvals).

**Response:** `200 OK`
```json
{
  "overdueReviews": [
    {
      "documentId": "uuid",
      "documentNumber": "DOC-2024-001",
      "title": "Quality Policy",
      "nextReviewAt": "2024-01-01T00:00:00Z",
      "daysOverdue": 45
    }
  ],
  "pendingApprovals": [
    {
      "documentId": "uuid",
      "documentNumber": "DOC-2024-002",
      "title": "Safety Procedure",
      "submittedAt": "2024-03-01T00:00:00Z",
      "submittedBy": "John Doe"
    }
  ]
}
```

### GET /api/documents/reviews/upcoming

Get documents with upcoming review dates.

**Query Parameters:**
- `days` (Integer, optional, default: 30) - Number of days to look ahead

**Response:** `200 OK`
```json
[
  {
    "documentId": "uuid",
    "documentNumber": "DOC-2024-001",
    "title": "Quality Policy",
    "nextReviewAt": "2026-04-01T00:00:00Z",
    "daysUntilReview": 20
  }
]
```

### GET /api/documents/workflow

Get workflow distribution statistics.

**Response:** `200 OK`
```json
{
  "draft": 10,
  "pending": 5,
  "active": 120,
  "archived": 15
}
```

### GET /api/documents/clauses/tree

Get clause tree with hierarchical structure for document linking.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "standardId": 1,
    "code": "4",
    "title": "Context of the Organisation",
    "description": "Understanding organizational context and stakeholder needs",
    "parentCode": null,
    "level": 1,
    "sortOrder": 4,
    "createdAt": "2024-01-01T00:00:00Z"
  },
  {
    "id": 2,
    "standardId": 1,
    "code": "4.1",
    "title": "Understanding the organisation and its context",
    "description": "Determine external and internal issues relevant to purpose",
    "parentCode": "4",
    "level": 2,
    "sortOrder": 1,
    "createdAt": "2024-01-01T00:00:00Z"
  }
]
```

**Notes:**
- Returns all ISO clauses with hierarchical information
- Use `parentCode` to build tree structure (null = top-level)
- Use `level` for indentation (1 = top-level, 2 = child, etc.)
- Use `sortOrder` for ordering within the same level
- Frontend can filter by `standardId` to show specific standards


### POST /api/documents/{id}/submit

Submit a document for approval (workflow transition: Draft → Pending).

**Path Parameters:**
- `id` (UUID) - Document UUID

**Response:** `200 OK` (returns updated DocumentResponse)

### POST /api/documents/{id}/approve

Approve a pending document (workflow transition: Pending → Active).

**Path Parameters:**
- `id` (UUID) - Document UUID

**Response:** `200 OK`

### POST /api/documents/{id}/reject

Reject a pending document (workflow transition: Pending → Draft).

**Path Parameters:**
- `id` (UUID) - Document UUID

**Response:** `200 OK`

### POST /api/documents/{id}/archive

Archive an active document (workflow transition: Active → Archived).

**Path Parameters:**
- `id` (UUID) - Document UUID

**Response:** `200 OK`

### POST /api/documents/{id}/versions

Upload a new document version file to S3.

**Path Parameters:**
- `id` (UUID) - Document UUID

**Request:** `multipart/form-data`
- `file` (MultipartFile, required) - Document file to upload

**Response:** `201 Created`
```json
{
  "id": 1,
  "documentId": "2b15d006-97f3-4695-9db1-f9fb3e16f8e9",
  "versionNumber": 3,
  "fileName": "quality-policy-v3.pdf",
  "fileSize": 2048576,
  "mimeType": "application/pdf",
  "s3Key": "documents/2b15d006-97f3-4695-9db1-f9fb3e16f8e9/v3/quality-policy-v3.pdf",
  "uploadedBy": "john.doe@company.com",
  "uploadedAt": "2024-01-20T14:30:00Z"
}
```

### GET /api/documents/{id}/versions

Get all versions for a document.

**Path Parameters:**
- `id` (UUID) - Document UUID

**Response:** `200 OK` (returns array of DocumentVersionResponse)


---

## Employees

Employee management with AWS Cognito integration.

### GET /api/employees

List all employees.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "cognitoSub": "abc123-def456",
    "cognitoUsername": "john.doe",
    "email": "john.doe@company.com",
    "firstName": "John",
    "lastName": "Doe",
    "roleId": 1,
    "roleName": "Administrator",
    "departmentId": 1,
    "departmentName": "Quality",
    "organisationId": 1,
    "organisationName": "ACME Corp",
    "siteId": 1,
    "siteName": "Headquarters",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

### GET /api/employees/{id}

Get a specific employee.

**Path Parameters:**
- `id` (Long) - Employee ID

**Response:** `200 OK`

### POST /api/employees

Create a new employee (also creates Cognito user).

**Request Body:**
```json
{
  "email": "john.doe@company.com",
  "firstName": "John",
  "lastName": "Doe",
  "roleId": 1,
  "departmentId": 1,
  "siteId": 1
}
```

**Field Descriptions:**
- `email` (String, required) - Employee email (used for Cognito username)
- `firstName` (String, required) - First name
- `lastName` (String, required) - Last name
- `roleId` (Long, required) - Role ID for RBAC
- `departmentId` (Long, optional) - Department ID
- `siteId` (Long, optional) - Site ID

**Response:** `201 Created`

**Note:** This endpoint creates a user in AWS Cognito and sends a temporary password via email.

### PUT /api/employees/{id}

Update an employee.

**Path Parameters:**
- `id` (Long) - Employee ID

**Request Body:** Same as POST

**Response:** `200 OK`

### DELETE /api/employees/{id}

Delete an employee (also disables Cognito user).

**Path Parameters:**
- `id` (Long) - Employee ID

**Response:** `204 No Content`


---

## Roles

RBAC role management with permissions.

### GET /api/roles

List all roles with user counts.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Administrator",
    "description": "Full system access",
    "permissions": [
      "MANAGE_USERS",
      "MANAGE_DOCUMENTS",
      "APPROVE_DOCUMENTS",
      "MANAGE_INCIDENTS"
    ],
    "userCount": 5
  }
]
```

### GET /api/roles/{id}

Get a specific role.

**Path Parameters:**
- `id` (Long) - Role ID

**Response:** `200 OK`

### POST /api/roles

Create a new role.

**Request Body:**
```json
{
  "name": "Quality Manager",
  "description": "Manages quality documentation and processes",
  "permissions": [
    "VIEW_DOCUMENTS",
    "CREATE_DOCUMENTS",
    "APPROVE_DOCUMENTS"
  ]
}
```

**Field Descriptions:**
- `name` (String, required) - Role name
- `description` (String, optional) - Role description
- `permissions` (List<String>, required) - List of permission codes

**Available Permissions:**
- `MANAGE_USERS` - Create, update, delete users
- `VIEW_DOCUMENTS` - View documents
- `CREATE_DOCUMENTS` - Create and edit documents
- `APPROVE_DOCUMENTS` - Approve/reject documents
- `MANAGE_INCIDENTS` - Full incident management
- `VIEW_INCIDENTS` - View incidents
- `CREATE_INCIDENTS` - Create incidents
- `MANAGE_SETTINGS` - System configuration

**Response:** `201 Created`

### PUT /api/roles/{id}

Update a role.

**Path Parameters:**
- `id` (Long) - Role ID

**Request Body:** Same as POST

**Response:** `200 OK`

### DELETE /api/roles/{id}

Delete a role.

**Path Parameters:**
- `id` (Long) - Role ID

**Response:** `204 No Content`

**Note:** Cannot delete roles that have users assigned.


---

## Incidents

Incident management with attachments, investigations, actions, and comments.

### GET /api/incidents

List incidents with optional filters and pagination.

**Query Parameters:**
- `status` (Long, optional) - Filter by status ID
- `severity` (Long, optional) - Filter by severity ID
- `department` (Long, optional) - Filter by department ID
- `site` (Long, optional) - Filter by site ID
- `search` (String, optional) - Search in title and incident number
- `page` (Integer, optional, default: 0) - Page number
- `size` (Integer, optional, default: 20) - Page size
- `sort` (String, optional, default: createdAt,desc) - Sort field and direction

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "id": "uuid",
      "incidentNumber": "INC-2024-001",
      "title": "Equipment malfunction in production area",
      "description": "Machine stopped unexpectedly during operation",
      "typeId": 1,
      "typeName": "Equipment Failure",
      "severityId": 2,
      "severityName": "Medium",
      "statusId": 1,
      "statusName": "Open",
      "departmentId": 3,
      "departmentName": "Production",
      "siteId": 1,
      "siteName": "Main Factory",
      "investigatorId": "user-456",
      "occurredAt": "2026-03-12T10:30:00Z",
      "reportedBy": "Jane Smith",
      "closedAt": null,
      "createdAt": "2026-03-12T11:00:00Z",
      "updatedAt": "2026-03-12T11:00:00Z"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

### GET /api/incidents/{incidentId}

Get incident detail including attachments, investigation, actions, and comments.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "incidentNumber": "INC-2024-001",
  "title": "Equipment malfunction",
  "description": "Machine stopped unexpectedly",
  "typeId": 1,
  "typeName": "Equipment Failure",
  "severityId": 2,
  "severityName": "Medium",
  "statusId": 2,
  "statusName": "Under Investigation",
  "departmentId": 3,
  "departmentName": "Production",
  "siteId": 1,
  "siteName": "Main Factory",
  "investigatorId": "user-456",
  "occurredAt": "2026-03-12T10:30:00Z",
  "reportedBy": "Jane Smith",
  "closedAt": null,
  "createdAt": "2026-03-12T11:00:00Z",
  "updatedAt": "2026-03-12T14:00:00Z",
  "attachments": [...],
  "investigation": {...},
  "actions": [...],
  "comments": [...]
}
```


### POST /api/incidents

Create a new incident.

**Request Body:**
```json
{
  "title": "Equipment malfunction in production area",
  "description": "Machine stopped unexpectedly during operation",
  "typeId": 1,
  "severityId": 2,
  "departmentId": 3,
  "siteId": 1,
  "occurredAt": "2026-03-12T10:30:00Z"
}
```

**Field Descriptions:**
- `title` (String, required) - Incident title
- `description` (String, optional) - Detailed description
- `typeId` (Long, required) - Incident type ID
- `severityId` (Long, required) - Severity ID (1=Low, 2=Medium, 3=High, 4=Critical)
- `departmentId` (Long, optional) - Department ID
- `siteId` (Long, optional) - Site ID
- `occurredAt` (OffsetDateTime, required) - When the incident occurred

**Response:** `201 Created`

### PUT /api/incidents/{incidentId}

Update an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request Body:**
```json
{
  "title": "Equipment malfunction in production area",
  "description": "Updated description with more details",
  "severityId": 3,
  "departmentId": 3,
  "investigatorId": "user-789"
}
```

**Field Descriptions:**
- `title` (String, optional) - Updated title
- `description` (String, optional) - Updated description
- `severityId` (Long, optional) - Updated severity ID
- `departmentId` (Long, optional) - Updated department ID
- `investigatorId` (String, optional) - Assigned investigator user ID

**Response:** `200 OK`

### PATCH /api/incidents/{incidentId}/status

Change incident status.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request Body:**
```json
{
  "statusId": 3
}
```

**Field Descriptions:**
- `statusId` (Long, required) - New status ID (1=Open, 2=Under Investigation, 3=Resolved, 4=Closed)

**Response:** `200 OK`


### PATCH /api/incidents/{incidentId}/assign

Assign investigator to incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request Body:**
```json
{
  "investigatorId": "user-456"
}
```

**Field Descriptions:**
- `investigatorId` (String, required) - User ID (Cognito sub) of the investigator

**Response:** `200 OK`

### PATCH /api/incidents/{incidentId}/close

Close an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Response:** `200 OK`

**Note:** Sets status to "Closed" and records the closure timestamp.

### GET /api/incidents/dashboard

Get incident dashboard metrics.

**Response:** `200 OK`
```json
{
  "totalIncidents": 150,
  "openIncidents": 25,
  "highSeverityIncidents": 8,
  "averageClosureTimeDays": 5.2
}
```

### POST /api/incidents/{incidentId}/attachments

Upload an attachment to an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request:** `multipart/form-data`
- `file` (MultipartFile, required) - File to upload

**Response:** `201 Created`
```json
{
  "id": 1,
  "incidentId": "uuid",
  "fileName": "photo-evidence.jpg",
  "fileSize": 1024000,
  "mimeType": "image/jpeg",
  "s3Key": "incidents/uuid/attachments/photo-evidence.jpg",
  "uploadedBy": "john.doe@company.com",
  "uploadedAt": "2026-03-12T14:30:00Z"
}
```

### GET /api/incidents/{incidentId}/attachments

List all attachments for an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Response:** `200 OK` (returns array of IncidentAttachmentResponse)


### POST /api/incidents/{incidentId}/comments

Add a comment to an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request Body:**
```json
{
  "comment": "Investigation is in progress, initial findings suggest maintenance issue"
}
```

**Field Descriptions:**
- `comment` (String, required) - Comment text

**Response:** `201 Created`
```json
{
  "id": 1,
  "incidentId": "uuid",
  "comment": "Investigation is in progress",
  "createdBy": "john.doe@company.com",
  "createdAt": "2026-03-12T15:00:00Z"
}
```

### GET /api/incidents/{incidentId}/comments

List all comments for an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Response:** `200 OK` (returns array of IncidentCommentResponse)

### POST /api/incidents/{incidentId}/investigation

Add investigation details to an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request Body:**
```json
{
  "investigatorId": "user-456",
  "analysisMethod": "5 Whys",
  "rootCause": "Equipment maintenance was overdue",
  "findings": "Detailed findings from the investigation including timeline and contributing factors"
}
```

**Field Descriptions:**
- `investigatorId` (String, required) - User ID of the investigator
- `analysisMethod` (String, optional) - Analysis method used (e.g., "5 Whys", "Fishbone", "Root Cause Analysis")
- `rootCause` (String, optional) - Identified root cause
- `findings` (String, optional) - Detailed investigation findings

**Response:** `201 Created`
```json
{
  "id": 1,
  "incidentId": "uuid",
  "investigatorId": "user-456",
  "analysisMethod": "5 Whys",
  "rootCause": "Equipment maintenance was overdue",
  "findings": "Detailed findings...",
  "createdAt": "2026-03-12T16:00:00Z"
}
```


### POST /api/incidents/{incidentId}/actions

Add a corrective action to an incident.

**Path Parameters:**
- `incidentId` (UUID) - Incident UUID

**Request Body:**
```json
{
  "title": "Schedule equipment maintenance",
  "description": "Perform full maintenance check on machine XYZ-123",
  "assignedTo": "user-789",
  "dueDate": "2026-03-20"
}
```

**Field Descriptions:**
- `title` (String, required) - Action title
- `description` (String, optional) - Detailed action description
- `assignedTo` (String, optional) - User ID assigned to this action
- `dueDate` (LocalDate, optional) - Due date for completion (format: YYYY-MM-DD)

**Response:** `201 Created`
```json
{
  "id": 1,
  "incidentId": "uuid",
  "title": "Schedule equipment maintenance",
  "description": "Perform full maintenance check",
  "assignedTo": "user-789",
  "dueDate": "2026-03-20",
  "status": "Pending",
  "completedAt": null,
  "createdAt": "2026-03-12T16:30:00Z"
}
```

### PATCH /api/incidents/actions/{actionId}

Update a corrective action status.

**Path Parameters:**
- `actionId` (Long) - Action ID

**Request Body:**
```json
{
  "status": "Completed",
  "completedAt": "2026-03-15T14:30:00Z"
}
```

**Field Descriptions:**
- `status` (String, optional) - Action status (e.g., "Pending", "In Progress", "Completed")
- `completedAt` (OffsetDateTime, optional) - Completion timestamp

**Response:** `200 OK`

---

## Error Response Format

All error responses follow this structure:

```json
{
  "timestamp": "2026-03-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'title': must not be blank",
  "path": "/api/incidents"
}
```

**Common Error Scenarios:**

- **400 Bad Request** - Invalid input, validation errors
- **401 Unauthorized** - Missing or invalid JWT token
- **403 Forbidden** - User lacks required permissions
- **404 Not Found** - Resource does not exist
- **409 Conflict** - Business rule violation (e.g., cannot delete role with assigned users)
- **500 Internal Server Error** - Unexpected server error


---

## Data Models Reference

### Document Workflow States

Documents follow this workflow:

```
Draft → Pending → Active → Archived
         ↓
       Draft (rejected)
```

**Status IDs:**
- 1 = Draft
- 2 = Active
- 3 = Pending
- 4 = Archived

### Incident Lifecycle

Incidents follow this lifecycle:

```
Open → Under Investigation → Resolved → Closed
```

**Status IDs:**
- 1 = Open
- 2 = Under Investigation
- 3 = Resolved
- 4 = Closed

**Severity Levels:**
- 1 = Low
- 2 = Medium
- 3 = High
- 4 = Critical

### ISO Standards

**Standard IDs:**
- 1 = ISO 9001:2015 (Quality Management)
- 2 = ISO 14001:2015 (Environmental Management)
- 3 = ISO 45001:2018 (Occupational Health & Safety)

### Clause Hierarchy

Clauses support hierarchical structure using `parentCode` and `level`:

```
4 (level 1)
├── 4.1 (level 2, parentCode: "4")
├── 4.2 (level 2, parentCode: "4")
│   ├── 4.2.1 (level 3, parentCode: "4.2")
│   └── 4.2.2 (level 3, parentCode: "4.2")
└── 4.3 (level 2, parentCode: "4")
```

### Date/Time Format

All timestamps use ISO 8601 format with timezone offset:

```
2026-03-12T10:30:00Z        (UTC)
2026-03-12T10:30:00-05:00   (EST)
```

Dates without time use:

```
2026-03-20
```

### UUID Format

Resource IDs for documents and incidents use UUID v4:

```
2b15d006-97f3-4695-9db1-f9fb3e16f8e9
```

---

## Testing Endpoints

You can test endpoints using curl:

```bash
# Get JWT token from Cognito first
TOKEN="your-jwt-token"

# List documents
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/documents

# Create incident
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test incident",
    "typeId": 1,
    "severityId": 2,
    "occurredAt": "2026-03-12T10:30:00Z"
  }' \
  http://localhost:8080/api/incidents

# Upload document version
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@document.pdf" \
  http://localhost:8080/api/documents/{id}/versions
```

