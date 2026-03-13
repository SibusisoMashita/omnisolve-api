package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when an incident's status changes (excluding closure, which has
 * its own {@link IncidentClosedEvent}).
 *
 * @param incidentId     the incident UUID
 * @param incidentNumber human-readable incident number
 * @param newStatusName  the new status name
 * @param organisationId the tenant the incident belongs to
 * @param changedBy      Cognito sub of the user who changed the status
 */
public record IncidentStatusChangedEvent(
        UUID incidentId,
        String incidentNumber,
        String newStatusName,
        Long organisationId,
        String changedBy) {
}
