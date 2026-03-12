package com.omnisolve.event.listener;

import com.omnisolve.event.DocumentApprovedEvent;
import com.omnisolve.event.DocumentRejectedEvent;
import com.omnisolve.event.DocumentSubmittedEvent;
import com.omnisolve.event.IncidentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Stub notification listener — ready to be wired to email, Slack, or
 * AWS SNS/SES when notification requirements are defined.
 *
 * <p>All methods are {@link Async} so that notification dispatch never blocks
 * the HTTP response. The current implementation only logs at DEBUG level;
 * replace the log statements with actual notification calls when ready.
 *
 * <p><strong>Future upgrade path:</strong>
 * <ul>
 *   <li>Inject a {@code NotificationService} that wraps AWS SES or SNS.</li>
 *   <li>Or publish to an SQS queue and handle in a Lambda function.</li>
 *   <li>No changes are needed at the event publishing sites in the services.</li>
 * </ul>
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentSubmittedEvent event) {
        log.debug("[NOTIFY-STUB] Document submitted for approval: documentNumber={}, organisationId={}",
                event.documentNumber(), event.organisationId());
        // TODO: notify document approvers in the organisation
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentApprovedEvent event) {
        log.debug("[NOTIFY-STUB] Document approved: documentNumber={}, approvedBy={}",
                event.documentNumber(), event.approvedBy());
        // TODO: notify document owner
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(DocumentRejectedEvent event) {
        log.debug("[NOTIFY-STUB] Document rejected: documentNumber={}, rejectedBy={}",
                event.documentNumber(), event.rejectedBy());
        // TODO: notify document owner with rejection reason
    }

    @EventListener
    @Async("omnisolveAsync")
    public void on(IncidentCreatedEvent event) {
        log.debug("[NOTIFY-STUB] New incident reported: incidentNumber={}, severity={}, organisationId={}",
                event.incidentNumber(), event.severityName(), event.organisationId());
        // TODO: notify HSE manager / safety team based on severity
    }
}
