package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when an incident is closed.
 *
 * @param incidentId     the incident UUID
 * @param incidentNumber human-readable incident number
 * @param organisationId the tenant the incident belongs to
 * @param closedBy       Cognito sub of the user who closed the incident
 */
public record IncidentClosedEvent(
        UUID incidentId,
        String incidentNumber,
        Long organisationId,
        String closedBy) {
}
