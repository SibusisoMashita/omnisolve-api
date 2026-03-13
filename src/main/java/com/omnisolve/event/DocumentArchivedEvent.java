package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when an active document is archived.
 *
 * @param documentId     the document UUID
 * @param documentNumber human-readable document number
 * @param organisationId the tenant the document belongs to
 * @param archivedBy     Cognito sub of the user who archived the document
 */
public record DocumentArchivedEvent(
        UUID documentId,
        String documentNumber,
        Long organisationId,
        String archivedBy) {
}
