package com.omnisolve.security;

import com.omnisolve.repository.EmployeeRepository;
import com.omnisolve.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Injectable facade over the Spring {@link SecurityContextHolder} that resolves the
 * currently authenticated user into a typed {@link AuthenticatedUser} value object.
 *
 * <p>This class replaces the static {@link AuthenticationUtil} helpers in the service
 * layer. Injecting it (rather than using static calls) makes services easier to unit-test
 * and removes hidden dependencies on {@code SecurityContextHolder} state.
 *
 * <p>As a side-effect of resolving the full user, it also populates {@link TenantContext}
 * and MDC keys ({@code userId}, {@code tenantId}) for the current thread so that
 * structured log statements automatically carry tenant context without additional
 * instrumentation in each service method.
 *
 * <p>{@link EmployeeRepository} is injected with {@code @Lazy} to break the
 * potential circular dependency that would otherwise arise from the security
 * configuration bean graph (JwtSecurityConfig → FirstLoginFilter → EmployeeService
 * → EmployeeRepository ← SecurityContextFacade).
 */
@Component
public class SecurityContextFacade {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextFacade.class);

    private static final String MDC_USER_ID  = "userId";
    private static final String MDC_TENANT_ID = "tenantId";

    private final EmployeeRepository employeeRepository;

    public SecurityContextFacade(@Lazy EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Resolves the full {@link AuthenticatedUser} for the current request, including
     * the {@code organisationId} resolved via the employees table.
     *
     * <p>As a side-effect this method:
     * <ul>
     *   <li>Sets {@link TenantContext#setOrganisationId(Long)} for the current thread.</li>
     *   <li>Populates MDC keys {@code userId} and {@code tenantId}.</li>
     * </ul>
     *
     * @throws ResponseStatusException with {@code 403 Forbidden} if the authenticated
     *         sub cannot be found in the employees table
     */
    public AuthenticatedUser currentUser() {
        String userId = extractUserId();
        String email    = extractClaim("email");
        String username = extractClaim("cognito:username");

        Long organisationId = resolveOrganisationId(userId);

        // Populate thread-local tenant context and MDC for downstream use
        if (organisationId != null) {
            TenantContext.setOrganisationId(organisationId);
            MDC.put(MDC_TENANT_ID, organisationId.toString());
        }
        MDC.put(MDC_USER_ID, userId);

        return new AuthenticatedUser(userId, email, username, organisationId);
    }

    /**
     * Returns only the user ID (JWT {@code sub} claim) without performing a DB lookup.
     * Use this in contexts where the organisation is not needed (e.g. audit trail
     * {@code performedBy} field when the full user is already resolved).
     */
    public String currentUserId() {
        return extractUserId();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getClaimAsString("sub");
            return sub != null ? sub : "system";
        }
        // UsernamePasswordAuthenticationToken used in tests — principal is the sub string
        if (auth.getPrincipal() instanceof String principal) {
            return principal;
        }
        String name = auth.getName();
        return name != null ? name : "system";
    }

    private String extractClaim(String claimName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString(claimName);
        }
        return null;
    }

    private Long resolveOrganisationId(String userId) {
        if ("system".equals(userId)) {
            log.debug("System principal detected — skipping organisation resolution");
            return null;
        }
        return employeeRepository.findByCognitoSub(userId)
                .map(employee -> employee.getOrganisation().getId())
                .orElseThrow(() -> {
                    log.warn("Authenticated user has no employee record: userId={}", userId);
                    return new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "User not associated with any organisation");
                });
    }
}
