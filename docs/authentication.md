# Authentication & Authorization

## Overview

OmniSolve uses AWS Cognito for authentication with JWT tokens. The backend is configured as an OAuth2 Resource Server that validates JWT tokens issued by Cognito.

## Configuration

### Environment Variables

The following environment variables configure Cognito authentication:

```bash
# Cognito Configuration
COGNITO_AUTHORITY=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_liURZm1XY
COGNITO_CLIENT_ID=c4af43ic7j8dfe70nr8cn79ei
COGNITO_DOMAIN=https://us-east-1liurzm1xy.auth.us-east-1.amazoncognito.com

# Enable/Disable JWT Authentication
JWT_ENABLED=true  # Set to false for local development without auth
```

### application.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${COGNITO_AUTHORITY}
          jwk-set-uri: ${COGNITO_AUTHORITY}/.well-known/jwks.json

app:
  security:
    jwt:
      enabled: ${JWT_ENABLED:true}
    cognito:
      audience: ${COGNITO_CLIENT_ID}
      domain: ${COGNITO_DOMAIN}
```

## Security Configuration

### Protected Endpoints

When `JWT_ENABLED=true`, the following security rules apply:

- **Public Endpoints** (no authentication required):
  - `/actuator/health`
  - `/health`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`

- **Protected Endpoints** (authentication required):
  - `/api/**` - All API endpoints require a valid JWT token

### Development Mode

Set `JWT_ENABLED=false` to disable authentication for local development:

```bash
export JWT_ENABLED=false
```

This allows all requests without requiring JWT tokens.

## JWT Token Structure

### Required Claims

The JWT token must contain the following claims:

- `sub` - Subject (user ID) - used as the authenticated user identifier
- `iss` - Issuer - must match the Cognito issuer URI
- `aud` or `client_id` - Audience - must match the Cognito client ID

### Optional Claims

- `email` - User's email address
- `cognito:username` - Cognito username
- `cognito:groups` - User groups (for future RBAC implementation)

## Using Authentication in Code

### Extract Authenticated User

Use the `AuthenticationUtil` class to extract user information from the JWT token:

```java
import com.omnisolve.security.AuthenticationUtil;

// Get the authenticated user ID (from 'sub' claim)
String userId = AuthenticationUtil.getAuthenticatedUserId();

// Get the user's email
String email = AuthenticationUtil.getAuthenticatedUserEmail();

// Get the Cognito username
String username = AuthenticationUtil.getAuthenticatedUsername();

// Get any custom claim
String customClaim = AuthenticationUtil.getClaim("custom:role");

// Check if request is authenticated
boolean isAuth = AuthenticationUtil.isAuthenticated();

// Get the full JWT token
Jwt jwt = AuthenticationUtil.getJwt();
```

### Controller Example

```java
@PostMapping
public DocumentResponse create(@RequestBody DocumentRequest request) {
    String userId = AuthenticationUtil.getAuthenticatedUserId();
    return documentService.create(request, userId);
}
```

### Fallback Behavior

When authentication is disabled (`JWT_ENABLED=false`) or no token is present:
- `getAuthenticatedUserId()` returns `"system"`
- Other methods return `null`

## Making Authenticated Requests

### Frontend Integration

Include the JWT token in the `Authorization` header:

```javascript
const response = await fetch('https://api.omnisolve.africa/api/documents', {
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  }
});
```

### cURL Example

```bash
curl -H "Authorization: Bearer eyJraWQiOiJ..." \
     https://api.omnisolve.africa/api/documents
```

### Postman

1. Select the request
2. Go to "Authorization" tab
3. Type: "Bearer Token"
4. Token: Paste your JWT token

## Token Validation

The backend validates JWT tokens using the following process:

1. **Signature Verification**: Validates the token signature using Cognito's public keys (JWKS)
2. **Issuer Validation**: Ensures the token was issued by the configured Cognito User Pool
3. **Audience Validation**: Verifies the token is intended for this application (client ID)
4. **Expiration Check**: Ensures the token has not expired

If any validation fails, the request is rejected with `401 Unauthorized`.

## Error Responses

### 401 Unauthorized

Returned when:
- No JWT token is provided
- Token is invalid or expired
- Token signature verification fails
- Issuer or audience validation fails

```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "status": 401
}
```

### 403 Forbidden

Returned when:
- Token is valid but user lacks required permissions (future RBAC implementation)

```json
{
  "error": "Forbidden",
  "message": "Access denied",
  "status": 403
}
```

## Testing

### Integration Tests

For integration tests, you can disable authentication:

```java
@SpringBootTest(properties = {
    "app.security.jwt.enabled=false"
})
class DocumentControllerIT {
    // Tests run without authentication
}
```

### Manual Testing with Real Tokens

1. Obtain a JWT token from Cognito (via login flow)
2. Set `JWT_ENABLED=true`
3. Include the token in API requests

## Future Enhancements

### Role-Based Access Control (RBAC)

Planned implementation:
- Extract roles from `cognito:groups` claim
- Use `@PreAuthorize` annotations on endpoints
- Implement role-based permissions (Admin, Author, Approver, Employee, Auditor)

Example:
```java
@PreAuthorize("hasRole('APPROVER')")
@PostMapping("/{id}/approve")
public DocumentResponse approve(@PathVariable UUID id) {
    // Only users with APPROVER role can access
}
```

### Audit Logging

All authenticated actions will be logged with:
- User ID (from JWT `sub` claim)
- User email (from JWT `email` claim)
- Action performed
- Timestamp
- IP address

## Troubleshooting

### "Full authentication is required"

**Cause**: No JWT token provided or token is invalid

**Solution**:
- Ensure the `Authorization: Bearer <token>` header is included
- Verify the token is not expired
- Check that the token is from the correct Cognito User Pool

### "Invalid token"

**Cause**: Token signature verification failed

**Solution**:
- Ensure `COGNITO_AUTHORITY` matches the User Pool that issued the token
- Verify the token has not been tampered with
- Check that the Cognito User Pool is accessible

### "The required audience is missing"

**Cause**: Token audience doesn't match configured client ID

**Solution**:
- Verify `COGNITO_CLIENT_ID` matches the client ID in the token
- Ensure the token was issued for the correct Cognito App Client

### Local Development Issues

**Solution**: Disable authentication for local development:
```bash
export JWT_ENABLED=false
```

## Security Best Practices

1. **Always use HTTPS** in production to protect tokens in transit
2. **Store tokens securely** on the client (e.g., httpOnly cookies, secure storage)
3. **Implement token refresh** to minimize exposure of long-lived tokens
4. **Set appropriate token expiration** (recommended: 1 hour for access tokens)
5. **Never log JWT tokens** in application logs
6. **Validate tokens on every request** (handled automatically by Spring Security)
7. **Use CORS configuration** to restrict which origins can access the API

## References

- [AWS Cognito Documentation](https://docs.aws.amazon.com/cognito/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [JWT.io](https://jwt.io/) - JWT token decoder and validator
