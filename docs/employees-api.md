# Employees API - Multi-Organisation SaaS Implementation

## Overview

The Employees API provides comprehensive employee management with multi-organisation SaaS architecture. Each employee belongs to an organisation, and all data access is automatically scoped to the authenticated user's organisation.

## Architecture

### Multi-Tenant Security Model

All employee operations enforce organisation-level isolation:

1. **Authentication**: User authenticates via AWS Cognito JWT
2. **Organisation Resolution**: System retrieves the authenticated user's employee record to determine their organisation
3. **Data Scoping**: All queries are automatically filtered by the user's organisation ID
4. **Access Control**: Users can only access employees within their own organisation

### Database Schema

```
organisations
├── id (BIGSERIAL PK)
├── name (VARCHAR)
├── created_at (TIMESTAMPTZ)
└── updated_at (TIMESTAMPTZ)

sites
├── id (BIGSERIAL PK)
├── organisation_id (FK → organisations)
├── name (VARCHAR)
├── created_at (TIMESTAMPTZ)
└── updated_at (TIMESTAMPTZ)

employees
├── id (BIGSERIAL PK)
├── cognito_sub (VARCHAR, UNIQUE)
├── cognito_username (VARCHAR, UNIQUE)
├── email (VARCHAR, UNIQUE)
├── first_name (VARCHAR)
├── last_name (VARCHAR)
├── role (VARCHAR)
├── department_id (FK → departments)
├── organisation_id (FK → organisations)
├── site_id (FK → sites)
├── status (VARCHAR)
├── created_at (TIMESTAMPTZ)
└── updated_at (TIMESTAMPTZ)
```

### Relationships

- **Employee → Organisation**: Many-to-One (required)
- **Employee → Site**: Many-to-One (optional)
- **Employee → Department**: Many-to-One (optional)
- **Organisation → Employees**: One-to-Many
- **Organisation → Sites**: One-to-Many
- **Site → Employees**: One-to-Many

## API Endpoints

### Base URL
```
/api/employees
```

All endpoints require JWT authentication via the `Authorization: Bearer <token>` header.

---

### 1. List Employees

**GET** `/api/employees`

Returns all employees in the authenticated user's organisation.

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "cognitoSub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "cognitoUsername": "john.doe@example.com",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "manager",
    "departmentId": 2,
    "departmentName": "Engineering",
    "organisationId": 1,
    "organisationName": "Acme Corp",
    "siteId": 3,
    "siteName": "Head Office",
    "status": "active",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-20T14:45:00Z"
  }
]
```

---

### 2. Create Employee

**POST** `/api/employees`

Creates a new employee in both Cognito and the local database.

**Request Body**:
```json
{
  "email": "jane.smith@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "employee",
  "departmentId": 2,
  "siteId": 3
}
```

**Field Descriptions**:
- `email` (required): Employee's email address (used as Cognito username)
- `firstName` (required): Employee's first name
- `lastName` (required): Employee's last name
- `role` (optional): Employee role (also used as Cognito group name)
- `departmentId` (optional): Department ID (must exist)
- `siteId` (optional): Site ID (must belong to the same organisation)

**Response**: `201 Created`
```json
{
  "id": 2,
  "cognitoSub": null,
  "cognitoUsername": "jane.smith@example.com",
  "email": "jane.smith@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "employee",
  "departmentId": 2,
  "departmentName": "Engineering",
  "organisationId": 1,
  "organisationName": "Acme Corp",
  "siteId": 3,
  "siteName": "Head Office",
  "status": "pending",
  "createdAt": "2024-01-21T09:15:00Z",
  "updatedAt": "2024-01-21T09:15:00Z"
}
```

**What Happens**:
1. Creates user in AWS Cognito with temporary password
2. Sends welcome email to user with password setup instructions
3. Saves employee record in local database with status "pending"
4. Adds user to Cognito group matching their role (if role is provided)
5. User's `cognitoSub` will be populated when they first log in

**Error Responses**:
- `409 Conflict`: Email already exists
- `404 Not Found`: Department or site not found
- `403 Forbidden`: Site belongs to different organisation

---

### 3. Update Employee

**PATCH** `/api/employees/{id}`

Updates employee details in the local database.

**Request Body**:
```json
{
  "firstName": "Jane",
  "lastName": "Smith-Johnson",
  "role": "manager",
  "departmentId": 3,
  "siteId": 4
}
```

**Response**: `200 OK`
```json
{
  "id": 2,
  "cognitoSub": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "cognitoUsername": "jane.smith@example.com",
  "email": "jane.smith@example.com",
  "firstName": "Jane",
  "lastName": "Smith-Johnson",
  "role": "manager",
  "departmentId": 3,
  "departmentName": "Operations",
  "organisationId": 1,
  "organisationName": "Acme Corp",
  "siteId": 4,
  "siteName": "Branch Office",
  "status": "active",
  "createdAt": "2024-01-21T09:15:00Z",
  "updatedAt": "2024-01-22T11:30:00Z"
}
```

**Notes**:
- Email cannot be changed (it's the Cognito username)
- Organisation ID is never changed (enforced by system)
- Only updates local database, not Cognito attributes

**Error Responses**:
- `404 Not Found`: Employee, department, or site not found
- `403 Forbidden`: Employee or site belongs to different organisation

---

### 4. Update Employee Status

**PATCH** `/api/employees/{id}/status`

Updates employee status and enables/disables the user in Cognito.

**Request Body**:
```json
{
  "status": "inactive"
}
```

**Valid Status Values**:
- `pending`: User created but hasn't completed signup
- `active`: User is active and can access the system
- `inactive`: User is disabled and cannot access the system

**Response**: `200 OK`
```json
{
  "id": 2,
  "cognitoSub": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "cognitoUsername": "jane.smith@example.com",
  "email": "jane.smith@example.com",
  "firstName": "Jane",
  "lastName": "Smith-Johnson",
  "role": "manager",
  "departmentId": 3,
  "departmentName": "Operations",
  "organisationId": 1,
  "organisationName": "Acme Corp",
  "siteId": 4,
  "siteName": "Branch Office",
  "status": "inactive",
  "createdAt": "2024-01-21T09:15:00Z",
  "updatedAt": "2024-01-22T15:00:00Z"
}
```

**What Happens**:
- `active`: Calls `AdminEnableUser` in Cognito
- `inactive`: Calls `AdminDisableUser` in Cognito
- `pending`: No Cognito action (user hasn't completed signup)

**Error Responses**:
- `404 Not Found`: Employee not found
- `403 Forbidden`: Employee belongs to different organisation
- `400 Bad Request`: Invalid status value

---

## Security & Multi-Tenancy

### Organisation Isolation

Every API call enforces organisation-level isolation:

```java
// 1. Get authenticated user's Cognito sub
String userId = AuthenticationUtil.getAuthenticatedUserId();

// 2. Load employee record
Employee authenticatedEmployee = employeeRepository.findByCognitoSub(userId);

// 3. Extract organisation ID
Long organisationId = authenticatedEmployee.getOrganisation().getId();

// 4. Scope all queries to this organisation
List<Employee> employees = employeeRepository.findByOrganisationId(organisationId);
```

### Access Control Rules

1. **List Employees**: Only returns employees from the user's organisation
2. **Create Employee**: New employee is automatically assigned to the user's organisation
3. **Update Employee**: Verifies employee belongs to user's organisation before updating
4. **Update Status**: Verifies employee belongs to user's organisation before changing status

### Cross-Organisation Protection

Attempting to access an employee from a different organisation returns:
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied to employee from different organisation"
}
```

---

## AWS Cognito Integration

### User Creation Flow

1. **AdminCreateUser**: Creates user in Cognito User Pool
   - Username: Employee's email
   - Attributes: email, given_name, family_name
   - Delivery: Welcome email with temporary password

2. **AdminAddUserToGroup**: Adds user to role-based group
   - Group name matches the employee's role
   - Used for future RBAC implementation

3. **User Signup**: User receives email and completes signup
   - Sets permanent password
   - Cognito generates `sub` (user ID)
   - On first login, `cognitoSub` is populated in employee record

### Status Management

- **Enable User**: `AdminEnableUser` - User can authenticate
- **Disable User**: `AdminDisableUser` - User cannot authenticate

### Configuration

Required environment variables:

```yaml
app:
  security:
    cognito:
      user-pool-id: us-east-1_XXXXXXXXX
```

---

## Implementation Details

### Code Structure

```
com.omnisolve
├── controller
│   └── EmployeeController.java          # REST endpoints
├── service
│   ├── EmployeeService.java             # Business logic & multi-tenancy
│   ├── CognitoService.java              # AWS Cognito integration
│   └── dto
│       ├── EmployeeRequest.java         # Create/update request
│       ├── EmployeeResponse.java        # API response
│       └── EmployeeStatusRequest.java   # Status update request
├── repository
│   ├── EmployeeRepository.java          # Data access
│   ├── OrganisationRepository.java
│   └── SiteRepository.java
├── domain
│   ├── Employee.java                    # JPA entity
│   ├── Organisation.java
│   └── Site.java
└── config
    └── CognitoConfig.java               # AWS SDK configuration
```

### Database Migrations

- `V4__create_organisation_and_employee_tables.sql`: Creates tables and indexes
- `V5__seed_organisations.sql`: Seeds initial organisation (optional)

### Dependencies

Added to `pom.xml`:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>cognitoidentityprovider</artifactId>
    <version>2.30.23</version>
</dependency>
```

---

## Testing

### Manual Testing with cURL

**1. List Employees**
```bash
curl -H "Authorization: Bearer <jwt-token>" \
     http://localhost:5000/api/employees
```

**2. Create Employee**
```bash
curl -X POST \
     -H "Authorization: Bearer <jwt-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "firstName": "Test",
       "lastName": "User",
       "role": "employee",
       "departmentId": 1
     }' \
     http://localhost:5000/api/employees
```

**3. Update Employee**
```bash
curl -X PATCH \
     -H "Authorization: Bearer <jwt-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "firstName": "Updated",
       "lastName": "Name",
       "role": "manager",
       "departmentId": 2
     }' \
     http://localhost:5000/api/employees/1
```

**4. Update Status**
```bash
curl -X PATCH \
     -H "Authorization: Bearer <jwt-token>" \
     -H "Content-Type: application/json" \
     -d '{"status": "inactive"}' \
     http://localhost:5000/api/employees/1/status
```

### Integration Testing

For integration tests, you can disable JWT authentication:

```java
@SpringBootTest(properties = {
    "app.security.jwt.enabled=false"
})
class EmployeeControllerIT {
    // Tests run without authentication
}
```

---

## Deployment Checklist

### 1. Database Migration
```bash
# Migrations run automatically on startup via Flyway
# Verify migrations:
./mvnw flyway:info
```

### 2. Environment Variables

Set in Elastic Beanstalk or your deployment environment:

```bash
COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
AWS_REGION=us-east-1
JWT_ENABLED=true
```

### 3. IAM Permissions

Ensure the application's IAM role has Cognito permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cognito-idp:AdminCreateUser",
        "cognito-idp:AdminEnableUser",
        "cognito-idp:AdminDisableUser",
        "cognito-idp:AdminAddUserToGroup"
      ],
      "Resource": "arn:aws:cognito-idp:us-east-1:ACCOUNT_ID:userpool/us-east-1_XXXXXXXXX"
    }
  ]
}
```

### 4. Initial Data Setup

Create initial organisation and admin employee:

```sql
-- Insert organisation
INSERT INTO organisations (name, created_at, updated_at)
VALUES ('Your Company', NOW(), NOW());

-- Insert admin employee (after they've logged in once to get cognito_sub)
INSERT INTO employees (
    cognito_sub, cognito_username, email, first_name, last_name,
    role, organisation_id, status, created_at, updated_at
)
VALUES (
    'cognito-sub-from-jwt', 'admin@yourcompany.com', 'admin@yourcompany.com',
    'Admin', 'User', 'admin', 1, 'active', NOW(), NOW()
);
```

---

## Troubleshooting

### "User not associated with any organisation"

**Cause**: Authenticated user doesn't have an employee record

**Solution**: Create an employee record for the user:
```sql
INSERT INTO employees (cognito_sub, email, organisation_id, status, created_at, updated_at)
VALUES ('user-cognito-sub', 'user@example.com', 1, 'active', NOW(), NOW());
```

### "Failed to create user in Cognito"

**Cause**: AWS credentials or permissions issue

**Solution**:
1. Verify IAM role has Cognito permissions
2. Check `COGNITO_USER_POOL_ID` is correct
3. Verify AWS region matches User Pool region

### "Access denied to employee from different organisation"

**Cause**: Attempting to access employee from another organisation

**Solution**: This is expected behavior - users can only access employees in their own organisation

---

## Future Enhancements

### Planned Features

1. **Role-Based Access Control (RBAC)**
   - Extract roles from Cognito groups
   - Implement `@PreAuthorize` annotations
   - Define permissions per role (Admin, Manager, Employee)

2. **Employee Invitations**
   - Send custom invitation emails
   - Track invitation status
   - Resend invitations

3. **Bulk Operations**
   - Import employees from CSV
   - Bulk status updates
   - Bulk role assignments

4. **Audit Trail**
   - Log all employee changes
   - Track who made changes and when
   - Compliance reporting

5. **Organisation Management API**
   - Create/update organisations
   - Manage sites
   - Organisation settings

---

## API Documentation

Interactive API documentation is available at:

```
http://localhost:5000/swagger-ui.html
```

The Employees API is documented under the "Employees" tag.
