package com.omnisolve.event.listener;

import com.omnisolve.audit.AuditEvent;
import com.omnisolve.audit.AuditService;
import com.omnisolve.event.DocumentApprovedEvent;
import com.omnisolve.event.DocumentArchivedEvent;
import com.omnisolve.event.DocumentRejectedEvent;
import com.omnisolve.event.DocumentSubmittedEvent;
import com.omnisolve.event.DocumentUploadedEvent;
import com.omnisolve.event.IncidentClosedEvent;
import com.omnisolve.event.IncidentCreatedEvent;
import com.omnisolve.event.IncidentStatusChangedEvent;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Translates domain events into {@link AuditEvent} records and persists them via
 * {@link AuditService}.
 *
 * <p>Using {@link EventListener} (Spring's synchronous listener by default) + the
 * async delegation inside {@link AuditService} ensures that:
 * <ol>
 *   <li>Events are dispatched on the same thread as the business operation (so
 *       correlation context is still in scope).</li>
 *   <li>The actual database write happens asynchronously, decoupled from the
 *       HTTP response.</li>
 * </ol>
 *
 * <p>All listener methods are annotated with {@link Async} so that slow audit
 * writes cannot delay the HTTP response even if the {@link AuditService} is
 * temporarily slow.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentSubmittedEvent event) {
        log.debug("Audit: DOCUMENT_SUBMITTED documentId={}", event.documentId());
        auditService.record(new AuditEvent(
                event.organisationId(), "DOCUMENT", event.documentId().toString(),
                "DOCUMENT_SUBMITTED", event.submittedBy(), OffsetDateTime.now(), null));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentApprovedEvent event) {
        log.debug("Audit: DOCUMENT_APPROVED documentId={}", event.documentId());
        auditService.record(new AuditEvent(
                event.organisationId(), "DOCUMENT", event.documentId().toString(),
                "DOCUMENT_APPROVED", event.approvedBy(), OffsetDateTime.now(), null));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentRejectedEvent event) {
        log.debug("Audit: DOCUMENT_REJECTED documentId={}", event.documentId());
        auditService.record(new AuditEvent(
                event.organisationId(), "DOCUMENT", event.documentId().toString(),
                "DOCUMENT_REJECTED", event.rejectedBy(), OffsetDateTime.now(), null));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentArchivedEvent event) {
        log.debug("Audit: DOCUMENT_ARCHIVED documentId={}", event.documentId());
        auditService.record(new AuditEvent(
                event.organisationId(), "DOCUMENT", event.documentId().toString(),
                "DOCUMENT_ARCHIVED", event.archivedBy(), OffsetDateTime.now(), null));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentUploadedEvent event) {
        log.debug("Audit: DOCUMENT_VERSION_UPLOADED documentId={} version={}", event.documentId(), event.versionNumber());
        String details = "{\"version\":" + event.versionNumber() + ",\"fileName\":\"" + event.fileName() + "\"}";
        auditService.record(new AuditEvent(
                event.organisationId(), "DOCUMENT", event.documentId().toString(),
                "DOCUMENT_VERSION_UPLOADED", event.uploadedBy(), OffsetDateTime.now(), details));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(IncidentCreatedEvent event) {
        log.debug("Audit: INCIDENT_CREATED incidentId={}", event.incidentId());
        String details = "{\"severity\":\"" + event.severityName() + "\",\"type\":\"" + event.typeName() + "\"}";
        auditService.record(new AuditEvent(
                event.organisationId(), "INCIDENT", event.incidentId().toString(),
                "INCIDENT_CREATED", event.reportedBy(), OffsetDateTime.now(), details));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(IncidentStatusChangedEvent event) {
        log.debug("Audit: INCIDENT_STATUS_CHANGED incidentId={} newStatus={}", event.incidentId(), event.newStatusName());
        String details = "{\"newStatus\":\"" + event.newStatusName() + "\"}";
        auditService.record(new AuditEvent(
                event.organisationId(), "INCIDENT", event.incidentId().toString(),
                "INCIDENT_STATUS_CHANGED", event.changedBy(), OffsetDateTime.now(), details));
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(IncidentClosedEvent event) {
        log.debug("Audit: INCIDENT_CLOSED incidentId={}", event.incidentId());
        auditService.record(new AuditEvent(
                event.organisationId(), "INCIDENT", event.incidentId().toString(),
                "INCIDENT_CLOSED", event.closedBy(), OffsetDateTime.now(), null));
    }
}
