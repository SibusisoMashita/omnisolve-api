# Multi-Tenant Setup Guide

## Quick Start

This guide walks you through setting up the multi-organisation SaaS architecture.

## Prerequisites

- Spring Boot application running
- PostgreSQL database configured
- AWS Cognito User Pool created
- IAM role with Cognito permissions

## Step 1: Run Database Migrations

The migrations will run automatically on application startup via Flyway.

Verify migrations:
```bash
./mvnw flyway:info
```

Expected migrations:
- `V1__init.sql` - Initial schema
- `V2__seed_data.sql` - Seed data
- `V3__seed_documents.sql` - Document seed data
- `V4__create_organisation_and_employee_tables.sql` - **NEW: Multi-tenant tables**
- `V5__seed_organisations.sql` - **NEW: Default organisation**

## Step 2: Configure Environment Variables

Add to your `application.yml` or environment:

```yaml
app:
  security:
    cognito:
      user-pool-id: ${COGNITO_USER_POOL_ID:us-east-1_XXXXXXXXX}
```

Or set as environment variable:
```bash
export COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
```

## Step 3: Configure IAM Permissions

Your application's IAM role needs Cognito permissions:

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
      "Resource": "arn:aws:cognito-idp:REGION:ACCOUNT_ID:userpool/USER_POOL_ID"
    }
  ]
}
```

## Step 4: Create Initial Organisation

If you didn't run `V5__seed_organisations.sql`, create an organisation manually:

```sql
INSERT INTO organisations (name, created_at, updated_at)
VALUES ('Your Company Name', NOW(), NOW());
```

## Step 5: Create Admin Employee

### Option A: Via API (Recommended)

1. First, create a user in Cognito manually or via AWS Console
2. Log in as that user to get a JWT token
3. Manually insert their employee record:

```sql
INSERT INTO employees (
    cognito_sub,
    cognito_username,
    email,
    first_name,
    last_name,
    role,
    organisation_id,
    status,
    created_at,
    updated_at
)
VALUES (
    'COGNITO_SUB_FROM_JWT',  -- Get from JWT token 'sub' claim
    'admin@yourcompany.com',
    'admin@yourcompany.com',
    'Admin',
    'User',
    'admin',
    1,  -- Organisation ID from step 4
    'active',
    NOW(),
    NOW()
);
```

### Option B: Bootstrap Script

Create a bootstrap script:

```sql
-- Create organisation
INSERT INTO organisations (id, name, created_at, updated_at)
VALUES (1, 'Acme Corp', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create site
INSERT INTO sites (id, organisation_id, name, created_at, updated_at)
VALUES (1, 1, 'Head Office', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create admin employee (update cognito_sub after first login)
INSERT INTO employees (
    email,
    first_name,
    last_name,
    role,
    organisation_id,
    status,
    created_at,
    updated_at
)
VALUES (
    'admin@acmecorp.com',
    'Admin',
    'User',
    'admin',
    1,
    'pending',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
```

## Step 6: Test the API

### 1. Get JWT Token

Log in via Cognito to get a JWT token.

### 2. Test List Employees

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:5000/api/employees
```

Expected response:
```json
[
  {
    "id": 1,
    "email": "admin@yourcompany.com",
    "firstName": "Admin",
    "lastName": "User",
    "role": "admin",
    "organisationId": 1,
    "organisationName": "Your Company Name",
    "status": "active"
  }
]
```

### 3. Create a New Employee

```bash
curl -X POST \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "email": "employee@yourcompany.com",
       "firstName": "John",
       "lastName": "Doe",
       "role": "employee",
       "departmentId": 1
     }' \
     http://localhost:5000/api/employees
```

Expected response:
```json
{
  "id": 2,
  "cognitoUsername": "employee@yourcompany.com",
  "email": "employee@yourcompany.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "employee",
  "organisationId": 1,
  "organisationName": "Your Company Name",
  "status": "pending"
}
```

The new employee will receive an email from Cognito with signup instructions.

## Step 7: Verify Multi-Tenancy

### Create Second Organisation

```sql
INSERT INTO organisations (name, created_at, updated_at)
VALUES ('Another Company', NOW(), NOW());
```

### Create Employee in Second Organisation

```sql
INSERT INTO employees (
    cognito_sub,
    email,
    first_name,
    last_name,
    organisation_id,
    status,
    created_at,
    updated_at
)
VALUES (
    'another-user-cognito-sub',
    'user@anothercompany.com',
    'Jane',
    'Smith',
    2,  -- Different organisation
    'active',
    NOW(),
    NOW()
);
```

### Test Isolation

Log in as the first admin and list employees:
```bash
curl -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
     http://localhost:5000/api/employees
```

You should only see employees from organisation 1, not organisation 2.

## Common Setup Issues

### Issue: "User not associated with any organisation"

**Cause**: The authenticated user doesn't have an employee record.

**Solution**: Create an employee record with the user's `cognito_sub`:

```sql
INSERT INTO employees (cognito_sub, email, organisation_id, status, created_at, updated_at)
VALUES ('USER_COGNITO_SUB', 'user@example.com', 1, 'active', NOW(), NOW());
```

To get the `cognito_sub`, decode the JWT token at https://jwt.io and look for the `sub` claim.

### Issue: "Failed to create user in Cognito"

**Cause**: Missing IAM permissions or incorrect User Pool ID.

**Solution**:
1. Verify `COGNITO_USER_POOL_ID` environment variable
2. Check IAM role has `cognito-idp:AdminCreateUser` permission
3. Verify AWS region matches User Pool region

### Issue: Migrations don't run

**Cause**: Flyway is disabled or migrations already applied.

**Solution**:
```bash
# Check migration status
./mvnw flyway:info

# Force repair if needed
./mvnw flyway:repair

# Run migrations manually
./mvnw flyway:migrate
```

## Production Deployment

### 1. Environment Variables

Set in Elastic Beanstalk or your deployment platform:

```bash
COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
AWS_REGION=us-east-1
JWT_ENABLED=true
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/omnisolve
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
```

### 2. IAM Role

Attach IAM policy to your EC2/ECS role:

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

### 3. Database Backup

Before deploying to production:

```bash
# Backup database
pg_dump -h your-rds-endpoint -U your-db-user omnisolve > backup.sql

# Test restore on staging
psql -h staging-rds-endpoint -U your-db-user omnisolve < backup.sql
```

### 4. Smoke Tests

After deployment:

1. Verify health endpoint: `GET /health`
2. Test authentication: `GET /api/employees` with JWT
3. Create test employee
4. Verify Cognito user created
5. Check CloudWatch logs for errors

## Next Steps

1. **Configure Cognito Groups**: Create groups matching your roles (admin, manager, employee)
2. **Set Up RBAC**: Implement role-based access control using Cognito groups
3. **Create Sites**: Add sites for each organisation
4. **Import Employees**: Bulk import existing employees
5. **Configure Email Templates**: Customize Cognito invitation emails

## Support

For issues or questions:
- Check logs: `tail -f /var/log/application.log`
- Review CloudWatch logs in AWS Console
- Verify database state: `SELECT * FROM employees;`
- Test Cognito: AWS Console → Cognito → User Pool → Users

## Reference

- [Employees API Documentation](./employees-api.md)
- [Authentication Documentation](./authentication.md)
- [API Documentation](./api.md)
- [Architecture Documentation](./architecture.md)
