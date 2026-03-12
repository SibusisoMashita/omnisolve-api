package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when a document is submitted for approval (Draft → Pending Approval).
 *
 * @param documentId     the document UUID
 * @param documentNumber human-readable document number
 * @param organisationId the tenant the document belongs to
 * @param submittedBy    Cognito sub of the user who submitted
 */
public record DocumentSubmittedEvent(
        UUID documentId,
        String documentNumber,
        Long organisationId,
        String submittedBy) {
}
