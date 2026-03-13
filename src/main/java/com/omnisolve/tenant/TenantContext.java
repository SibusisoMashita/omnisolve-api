package com.omnisolve.tenant;

/**
 * Thread-local holder for the current tenant's organisation ID.
 *
 * <p>Populated by {@link com.omnisolve.security.SecurityContextFacade#currentUser()}
 * as a side-effect of the first organisation resolution in a request. Once set,
 * cross-cutting concerns such as the audit aspect can read it without triggering
 * an additional database lookup.
 *
 * <p><strong>Lifecycle:</strong> The value is set by {@code SecurityContextFacade} at
 * service entry and cleared by {@link com.omnisolve.observability.RequestLoggingFilter}
 * in its {@code finally} block so that the thread is clean before it is returned to
 * the pool.
 *
 * <p>This class is intentionally non-Spring-managed (plain static utility) so it can
 * be used from AOP advice and other infrastructure code without circular bean
 * dependencies.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> ORGANISATION_ID = new ThreadLocal<>();

    private TenantContext() {
        // Utility class — not instantiable
    }

    /**
     * Sets the organisation ID for the current thread.
     *
     * @param organisationId the tenant's organisation row ID
     */
    public static void setOrganisationId(Long organisationId) {
        ORGANISATION_ID.set(organisationId);
    }

    /**
     * Returns the organisation ID for the current thread, or {@code null} if not set.
     */
    public static Long getOrganisationId() {
        return ORGANISATION_ID.get();
    }

    /**
     * Clears the stored organisation ID from the current thread.
     * Must be called in a {@code finally} block to prevent thread-pool leakage.
     */
    public static void clear() {
        ORGANISATION_ID.remove();
    }
}
