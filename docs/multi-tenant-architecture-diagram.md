# Multi-Tenant Architecture Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                         │
│                     http://localhost:5173                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTPS + JWT Token
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                    Spring Boot API (Port 5000)                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              JWT Security Filter Chain                     │  │
│  │  - Validates JWT signature                                 │  │
│  │  - Checks issuer & audience                                │  │
│  │  - Extracts user identity (sub claim)                      │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────▼───────────────────────────────┐  │
│  │              EmployeeController                            │  │
│  │  GET    /api/employees                                     │  │
│  │  POST   /api/employees                                     │  │
│  │  PATCH  /api/employees/{id}                                │  │
│  │  PATCH  /api/employees/{id}/status                         │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────▼───────────────────────────────┐  │
│  │              EmployeeService                               │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  Multi-Tenant Security Layer                        │  │  │
│  │  │  1. Get authenticated user's cognito_sub           │  │  │
│  │  │  2. Load employee record                            │  │  │
│  │  │  3. Extract organisation_id                         │  │  │
│  │  │  4. Scope all queries to this organisation          │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                              │                                   │
│         ┌────────────────────┼────────────────────┐             │
│         │                    │                    │             │
│  ┌──────▼──────┐    ┌────────▼────────┐   ┌──────▼──────┐     │
│  │  Employee   │    │   Cognito       │   │  Department │     │
│  │  Repository │    │   Service       │   │  Repository │     │
│  └──────┬──────┘    └────────┬────────┘   └──────┬──────┘     │
│         │                    │                    │             │
└─────────┼────────────────────┼────────────────────┼─────────────┘
          │                    │                    │
          │                    │                    │
┌─────────▼────────────────────┼────────────────────▼─────────────┐
│                PostgreSQL Database                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │organisations │  │    sites     │  │  employees   │          │
│  │──────────────│  │──────────────│  │──────────────│          │
│  │ id           │  │ id           │  │ id           │          │
│  │ name         │◄─┤ org_id (FK)  │◄─┤ org_id (FK)  │          │
│  └──────────────┘  └──────────────┘  │ site_id (FK) │          │
│                                       │ dept_id (FK) │          │
│                                       │ cognito_sub  │          │
│                                       │ email        │          │
│                                       │ status       │          │
│                                       └──────────────┘          │
└──────────────────────────────────────────────────────────────────┘
                             │
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      AWS Cognito User Pool                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Users                                                    │  │
│  │  - sub (UUID)                                             │  │
│  │  - email                                                  │  │
│  │  - given_name                                             │  │
│  │  - family_name                                            │  │
│  │  - enabled/disabled                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Groups (for RBAC)                                        │  │
│  │  - admin                                                  │  │
│  │  - manager                                                │  │
│  │  - employee                                               │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

## Request Flow: List Employees

```
┌──────────┐
│  Client  │
└────┬─────┘
     │
     │ 1. GET /api/employees
     │    Authorization: Bearer eyJhbGc...
     │
┌────▼─────────────────────────────────────────────────────────┐
│  Spring Security Filter                                       │
│  - Validates JWT token                                        │
│  - Extracts claims: sub="a1b2c3d4-..."                        │
│  - Sets SecurityContext                                       │
└────┬─────────────────────────────────────────────────────────┘
     │
┌────▼─────────────────────────────────────────────────────────┐
│  EmployeeController.getAll()                                  │
│  - Calls service.list()                                       │
└────┬─────────────────────────────────────────────────────────┘
     │
┌────▼─────────────────────────────────────────────────────────┐
│  EmployeeService.list()                                       │
│                                                               │
│  2. userId = AuthenticationUtil.getAuthenticatedUserId()      │
│     → "a1b2c3d4-e5f6-7890-abcd-ef1234567890"                 │
│                                                               │
│  3. employee = employeeRepository.findByCognitoSub(userId)    │
│     → Employee{id=1, org_id=5, ...}                           │
│                                                               │
│  4. organisationId = employee.getOrganisation().getId()       │
│     → 5                                                       │
│                                                               │
│  5. employees = employeeRepository                            │
│                 .findByOrganisationId(organisationId)         │
│     → [Employee{org_id=5}, Employee{org_id=5}, ...]          │
│                                                               │
│  6. return employees.stream().map(toResponse)                 │
└────┬─────────────────────────────────────────────────────────┘
     │
┌────▼─────────────────────────────────────────────────────────┐
│  Response: 200 OK                                             │
│  [                                                            │
│    {                                                          │
│      "id": 1,                                                 │
│      "organisationId": 5,                                     │
│      "organisationName": "Acme Corp",                         │
│      ...                                                      │
│    }                                                          │
│  ]                                                            │
└───────────────────────────────────────────────────────────────┘
```

## Request Flow: Create Employee

```
┌──────────┐
│  Client  │
└────┬─────┘
     │
     │ 1. POST /api/employees
     │    Authorization: Bearer eyJhbGc...
     │    Body: {"email": "new@example.com", ...}
     │
┌────▼─────────────────────────────────────────────────────────┐
│  EmployeeController.create(request)                           │
└────┬─────────────────────────────────────────────────────────┘
     │
┌────▼─────────────────────────────────────────────────────────┐
│  EmployeeService.create(request)                              │
│                                                               │
│  2. organisationId = getAuthenticatedUserOrganisationId()     │
│     → 5                                                       │
│                                                               │
│  3. organisation = organisationRepository.findById(5)         │
│     → Organisation{id=5, name="Acme Corp"}                    │
│                                                               │
│  4. cognitoUsername = cognitoService.createUser(...)          │
│     ┌──────────────────────────────────────────────────┐     │
│     │  CognitoService.createUser()                     │     │
│     │  - Calls AdminCreateUser API                     │     │
│     │  - Sets email, given_name, family_name           │     │
│     │  - Sends welcome email                           │     │
│     │  → Returns "new@example.com"                     │     │
│     └──────────────────────────────────────────────────┘     │
│                                                               │
│  5. employee = new Employee()                                 │
│     employee.setEmail("new@example.com")                      │
│     employee.setCognitoUsername("new@example.com")            │
│     employee.setOrganisation(organisation)  // org_id = 5     │
│     employee.setStatus("pending")                             │
│                                                               │
│  6. saved = employeeRepository.save(employee)                 │
│     → Employee{id=10, org_id=5, status="pending"}            │
│                                                               │
│  7. cognitoService.addUserToGroup(username, role)             │
│     (optional, based on role)                                 │
│                                                               │
│  8. return toResponse(saved)                                  │
└────┬─────────────────────────────────────────────────────────┘
     │
┌────▼─────────────────────────────────────────────────────────┐
│  Response: 201 Created                                        │
│  {                                                            │
│    "id": 10,                                                  │
│    "cognitoUsername": "new@example.com",                      │
│    "organisationId": 5,                                       │
│    "status": "pending"                                        │
│  }                                                            │
└───────────────────────────────────────────────────────────────┘
     │
     │ Meanwhile...
     │
┌────▼─────────────────────────────────────────────────────────┐
│  AWS Cognito                                                  │
│  - Sends welcome email to new@example.com                     │
│  - Email contains temporary password                          │
│  - User clicks link and sets permanent password               │
│  - On first login, cognito_sub is generated                   │
└───────────────────────────────────────────────────────────────┘
```

## Multi-Tenant Isolation

```
┌─────────────────────────────────────────────────────────────┐
│                    Organisation 1                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Employees                                           │   │
│  │  - employee1@org1.com (org_id=1)                     │   │
│  │  - employee2@org1.com (org_id=1)                     │   │
│  │  - employee3@org1.com (org_id=1)                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                             │
                             │ ❌ CANNOT ACCESS
                             │
┌─────────────────────────────────────────────────────────────┐
│                    Organisation 2                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Employees                                           │   │
│  │  - employee1@org2.com (org_id=2)                     │   │
│  │  - employee2@org2.com (org_id=2)                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

User from Org 1 tries to access employee from Org 2:
┌─────────────────────────────────────────────────────────────┐
│  GET /api/employees/999                                      │
│  (where employee 999 belongs to Org 2)                       │
│                                                              │
│  1. Authenticated user's org_id = 1                          │
│  2. Employee 999's org_id = 2                                │
│  3. 1 ≠ 2                                                    │
│  4. Response: 403 Forbidden                                  │
│     "Access denied to employee from different organisation"  │
└─────────────────────────────────────────────────────────────┘
```

## Database Relationships

```
┌──────────────────┐
│  organisations   │
│  ──────────────  │
│  id (PK)         │
│  name            │
└────────┬─────────┘
         │
         │ 1:N
         │
    ┌────┴────┬──────────────────────────────┐
    │         │                              │
┌───▼─────┐   │                         ┌────▼────────┐
│  sites  │   │                         │  employees  │
│  ─────  │   │                         │  ─────────  │
│  id     │   │                         │  id         │
│  org_id │◄──┘                         │  org_id     │
│  name   │                             │  site_id    │◄─┐
└───┬─────┘                             │  dept_id    │  │
    │                                   │  email      │  │
    │ 1:N                               │  status     │  │
    │                                   └─────────────┘  │
    └──────────────────────────────────────────────────┘
```

## Status Synchronization

```
┌─────────────────────────────────────────────────────────────┐
│  PATCH /api/employees/1/status                               │
│  Body: {"status": "inactive"}                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  EmployeeService.updateStatus(1, "inactive")                 │
│                                                              │
│  1. Load employee from database                              │
│     employee = employeeRepository.findById(1)                │
│                                                              │
│  2. Verify organisation access                               │
│     verifyOrganisationAccess(employee)                       │
│                                                              │
│  3. Update Cognito user status                               │
│     if (status == "inactive")                                │
│       cognitoService.disableUser(employee.cognitoUsername)   │
│     else if (status == "active")                             │
│       cognitoService.enableUser(employee.cognitoUsername)    │
│                                                              │
│  4. Update local database                                    │
│     employee.setStatus("inactive")                           │
│     employeeRepository.save(employee)                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Result:                                                     │
│  - Database: status = "inactive"                             │
│  - Cognito: user.enabled = false                             │
│  - User cannot log in                                        │
└─────────────────────────────────────────────────────────────┘
```

## Key Security Points

1. **Organisation ID is NEVER user-supplied**
   - Always derived from authenticated user's employee record
   - Cannot be changed via API

2. **All queries are automatically scoped**
   - `findByOrganisationId(organisationId)`
   - No way to bypass organisation filter

3. **Cross-organisation access is blocked**
   - Verified before any update/delete operation
   - Returns 403 Forbidden

4. **JWT authentication required**
   - All endpoints under `/api/employees` require valid JWT
   - Token validated by Spring Security

5. **Cognito integration**
   - User creation synced with Cognito
   - Status changes synced with Cognito enabled/disabled
   - Role-based group assignment
