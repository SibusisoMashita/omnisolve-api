package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when a new incident is reported.
 *
 * @param incidentId     the incident UUID
 * @param incidentNumber human-readable incident number
 * @param severityName   severity label (e.g. "High", "Critical")
 * @param typeName       incident type label (e.g. "Injury", "Near Miss")
 * @param organisationId the tenant the incident belongs to
 * @param reportedBy     Cognito sub of the user who reported the incident
 */
public record IncidentCreatedEvent(
        UUID incidentId,
        String incidentNumber,
        String severityName,
        String typeName,
        Long organisationId,
        String reportedBy) {
}
