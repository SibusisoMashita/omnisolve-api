package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when a document is approved and transitions to Active status.
 *
 * @param documentId     the document UUID
 * @param documentNumber human-readable document number
 * @param organisationId the tenant the document belongs to
 * @param approvedBy     Cognito sub of the approving user
 */
public record DocumentApprovedEvent(
        UUID documentId,
        String documentNumber,
        Long organisationId,
        String approvedBy) {
}
