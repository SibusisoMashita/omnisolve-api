package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when a pending document is rejected and transitions back to Draft.
 *
 * @param documentId     the document UUID
 * @param documentNumber human-readable document number
 * @param organisationId the tenant the document belongs to
 * @param rejectedBy     Cognito sub of the rejecting user
 */
public record DocumentRejectedEvent(
        UUID documentId,
        String documentNumber,
        Long organisationId,
        String rejectedBy) {
}
