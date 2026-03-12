package com.omnisolve.security;

/**
 * Immutable value object representing the currently authenticated user.
 *
 * <p>Carries identity and tenant context extracted from the JWT token and the
 * local employees table. Passed through the service layer instead of repeatedly
 * calling static helpers or performing redundant DB lookups per request.
 *
 * @param userId         Cognito {@code sub} claim — stable, opaque user identifier
 * @param email          JWT {@code email} claim, may be {@code null} in dev/test mode
 * @param username       JWT {@code cognito:username} claim, may be {@code null}
 * @param organisationId Row ID from the {@code organisations} table resolved via the
 *                       employees table; {@code null} only when JWT auth is disabled
 *                       and no employee record exists (e.g. system tasks)
 */
public record AuthenticatedUser(
        String userId,
        String email,
        String username,
        Long organisationId) {

    /**
     * Returns {@code true} when the user was resolved from a real JWT token and
     * has a linked employee record (i.e. is a real tenant user, not the fallback
     * {@code "system"} principal used in development mode).
     */
    public boolean isTenantUser() {
        return organisationId != null && !"system".equals(userId);
    }
}
