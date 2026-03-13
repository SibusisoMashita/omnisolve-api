package com.omnisolve.audit;

import java.time.OffsetDateTime;

/**
 * Immutable value object representing a single business operation that must be
 * recorded in the audit trail.
 *
 * <p>Created by {@link AuditAspect} after a successful {@link Auditable}-annotated
 * method call and passed to {@link AuditService#record(AuditEvent)} for persistence.
 *
 * @param organisationId tenant scope; may be {@code null} for system operations
 * @param entityType     the type of the affected entity (e.g. {@code "DOCUMENT"})
 * @param entityId       string representation of the entity's primary key
 * @param action         the operation performed (e.g. {@code "DOCUMENT_APPROVED"})
 * @param performedBy    Cognito sub of the user who triggered the operation
 * @param performedAt    wall-clock timestamp of the operation
 * @param details        optional JSON metadata about the operation; may be {@code null}
 */
public record AuditEvent(
        Long organisationId,
        String entityType,
        String entityId,
        String action,
        String performedBy,
        OffsetDateTime performedAt,
        String details) {
}
