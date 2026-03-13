package com.omnisolve.audit;

import com.omnisolve.domain.AuditLog;
import com.omnisolve.domain.Organisation;
import com.omnisolve.repository.AuditLogRepository;
import com.omnisolve.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists {@link AuditEvent} records to the {@code audit_logs} table.
 *
 * <p>All writes are executed <em>asynchronously</em> on the {@code omnisolveAsync}
 * executor so that a slow database write never delays the business HTTP response.
 *
 * <p>The method runs in {@link Propagation#REQUIRES_NEW} so that it opens its own
 * transaction, independent of the caller's transaction. This guarantees that the
 * audit record is committed even if the calling transaction is rolled back — a
 * failed business operation should still produce an audit trail entry.
 *
 * <p>Exceptions thrown during audit persistence are caught and logged; they must
 * never propagate to the caller because audit failures should not surface as HTTP
 * 500 errors.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final OrganisationRepository organisationRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            OrganisationRepository organisationRepository) {
        this.auditLogRepository = auditLogRepository;
        this.organisationRepository = organisationRepository;
    }

    /**
     * Persists an audit event record asynchronously.
     *
     * @param event the audit event to record; must not be {@code null}
     */
    @Async("omnisolveAsync")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        try {
            AuditLog entry = new AuditLog();

            if (event.organisationId() != null) {
                organisationRepository.findById(event.organisationId())
                        .ifPresent(entry::setOrganisation);
            }

            entry.setEntityName(event.entityType());
            entry.setEntityId(event.entityId() != null ? event.entityId() : "unknown");
            entry.setAction(event.action());
            entry.setDetails(event.details());
            entry.setPerformedBy(event.performedBy());
            entry.setPerformedAt(event.performedAt());

            auditLogRepository.save(entry);
            log.debug("Audit record persisted: action={}, entityType={}, entityId={}, performedBy={}",
                    event.action(), event.entityType(), event.entityId(), event.performedBy());

        } catch (Exception ex) {
            // Audit failure must never propagate — log and swallow
            log.error("Failed to persist audit record: action={}, entityType={}, entityId={}, error={}",
                    event.action(), event.entityType(), event.entityId(), ex.getMessage(), ex);
        }
    }
}
