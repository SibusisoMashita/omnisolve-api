package com.omnisolve.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging.
 *
 * <p>When a method annotated with {@code @Auditable} completes successfully,
 * {@link AuditAspect} intercepts the call and delegates to {@link AuditService}
 * to persist an immutable record in the {@code audit_logs} table.
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * @Auditable(action = "DOCUMENT_APPROVED", entityType = "DOCUMENT")
 * @Transactional
 * public DocumentResponse approve(UUID id, String userId) { ... }
 * }</pre>
 *
 * <p>The audit record is written asynchronously so that a slow database write
 * never delays the business response.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * Human-readable action label stored in the {@code action} column.
     * Use uppercase snake-case, e.g. {@code "DOCUMENT_APPROVED"}.
     */
    String action();

    /**
     * Entity type stored in the {@code entity_name} column.
     * Use uppercase, e.g. {@code "DOCUMENT"} or {@code "INCIDENT"}.
     */
    String entityType();
}
