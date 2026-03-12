package com.omnisolve.controller;

import com.omnisolve.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class IncidentControllerIT extends IntegrationTestBase {

    @Test
    void shouldListIncidents() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/incidents", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("content"));
        assertTrue(response.getBody().containsKey("totalElements"));
        assertTrue(response.getBody().containsKey("totalPages"));
    }

    @Test
    void shouldListIncidentsWithFilters() {
        String url = "/api/incidents?search=test&page=0&size=10";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("content"));
    }

    @Test
    void shouldCreateIncident() {
        Map<String, Object> request = Map.of(
                "title", "Equipment Malfunction",
                "description", "Machine stopped unexpectedly during operation",
                "typeId", 1,
                "severityId", 2,
                "departmentId", 1,
                "siteId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/incidents", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertNotNull(response.getBody().get("incidentNumber"));
        assertEquals("Equipment Malfunction", response.getBody().get("title"));
        assertEquals("Reported", response.getBody().get("statusName"));
        
        // Verify incident number format (e.g., SAF-2026-001)
        String incidentNumber = response.getBody().get("incidentNumber").toString();
        assertTrue(incidentNumber.matches("^[A-Z]{3}-\\d{4}-\\d{3}$"),
                "Incident number should match format PREFIX-YEAR-SEQUENCE");
    }

    @Test
    void shouldGetIncidentById() {
        // First create an incident
        Map<String, Object> createRequest = Map.of(
                "title", "Test Incident for Retrieval",
                "description", "Testing get by ID",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        String incidentId = createResponse.getBody().get("id").toString();

        // Then retrieve it
        ResponseEntity<Map> getResponse = restTemplate.getForEntity("/api/incidents/" + incidentId, Map.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(incidentId, getResponse.getBody().get("id"));
        assertEquals("Test Incident for Retrieval", getResponse.getBody().get("title"));

        // Verify all required fields are present
        assertNotNull(getResponse.getBody().get("typeId"));
        assertNotNull(getResponse.getBody().get("typeName"));
        assertNotNull(getResponse.getBody().get("severityId"));
        assertNotNull(getResponse.getBody().get("severityName"));
        assertNotNull(getResponse.getBody().get("statusId"));
        assertNotNull(getResponse.getBody().get("statusName"));
        assertNotNull(getResponse.getBody().get("occurredAt"));
        assertNotNull(getResponse.getBody().get("reportedBy"));
        assertNotNull(getResponse.getBody().get("createdAt"));
        assertNotNull(getResponse.getBody().get("updatedAt"));

        // Verify related data arrays
        assertTrue(getResponse.getBody().containsKey("attachments"));
        assertTrue(getResponse.getBody().containsKey("actions"));
        assertTrue(getResponse.getBody().containsKey("comments"));
    }

    @Test
    void shouldUpdateIncident() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Original Title",
                "description", "Original description",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Update incident
        Map<String, Object> updateRequest = Map.of(
                "title", "Updated Title",
                "description", "Updated description",
                "severityId", 3
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateRequest);
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/incidents/" + incidentId,
                HttpMethod.PUT,
                requestEntity,
                Map.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertNotNull(updateResponse.getBody());
        assertEquals("Updated Title", updateResponse.getBody().get("title"));
        assertEquals("Updated description", updateResponse.getBody().get("description"));
    }

    @Test
    void shouldChangeIncidentStatus() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Status Change Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Change status to "Under Investigation" (assuming statusId 2)
        Map<String, Object> statusRequest = Map.of("statusId", 2);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(statusRequest);
        ResponseEntity<Map> statusResponse = restTemplate.exchange(
                "/api/incidents/" + incidentId + "/status",
                HttpMethod.PATCH,
                requestEntity,
                Map.class
        );

        assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
        assertNotNull(statusResponse.getBody());
        assertEquals(2, statusResponse.getBody().get("statusId"));
    }

    @Test
    void shouldAssignInvestigator() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Assign Investigator Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Assign investigator
        Map<String, Object> assignRequest = Map.of("investigatorId", "investigator-user-456");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(assignRequest);
        ResponseEntity<Map> assignResponse = restTemplate.exchange(
                "/api/incidents/" + incidentId + "/assign",
                HttpMethod.PATCH,
                requestEntity,
                Map.class
        );

        assertEquals(HttpStatus.OK, assignResponse.getStatusCode());
        assertNotNull(assignResponse.getBody());
        assertEquals("investigator-user-456", assignResponse.getBody().get("investigatorId"));
    }

    @Test
    void shouldCloseIncident() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Close Incident Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Close incident
        HttpEntity<Void> requestEntity = new HttpEntity<>(null);
        ResponseEntity<Map> closeResponse = restTemplate.exchange(
                "/api/incidents/" + incidentId + "/close",
                HttpMethod.PATCH,
                requestEntity,
                Map.class
        );

        assertEquals(HttpStatus.OK, closeResponse.getStatusCode());
        assertNotNull(closeResponse.getBody());
        assertEquals("Closed", closeResponse.getBody().get("statusName"));
        assertNotNull(closeResponse.getBody().get("closedAt"));
    }

    @Test
    void shouldUploadAttachment() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Attachment Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Upload attachment
        byte[] fileContent = "Test file content".getBytes();
        ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return "test-document.txt";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(
                "/api/incidents/" + incidentId + "/attachments",
                requestEntity,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, uploadResponse.getStatusCode());
        assertNotNull(uploadResponse.getBody());
        assertNotNull(uploadResponse.getBody().get("id"));
        assertEquals("test-document.txt", uploadResponse.getBody().get("fileName"));
        assertNotNull(uploadResponse.getBody().get("s3Key"));
        assertNotNull(uploadResponse.getBody().get("uploadedBy"));
        assertNotNull(uploadResponse.getBody().get("uploadedAt"));
    }

    @Test
    void shouldListAttachments() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "List Attachments Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // List attachments (should be empty initially)
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
                "/api/incidents/" + incidentId + "/attachments",
                List.class
        );

        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertNotNull(listResponse.getBody());
        assertTrue(listResponse.getBody().isEmpty());
    }

    @Test
    void shouldAddComment() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Comment Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Add comment
        Map<String, Object> commentRequest = Map.of("comment", "This is a test comment");

        ResponseEntity<Map> commentResponse = restTemplate.postForEntity(
                "/api/incidents/" + incidentId + "/comments",
                commentRequest,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, commentResponse.getStatusCode());
        assertNotNull(commentResponse.getBody());
        assertNotNull(commentResponse.getBody().get("id"));
        assertEquals("This is a test comment", commentResponse.getBody().get("comment"));
        assertNotNull(commentResponse.getBody().get("createdBy"));
        assertNotNull(commentResponse.getBody().get("createdAt"));
    }

    @Test
    void shouldListComments() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "List Comments Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Add a comment
        Map<String, Object> commentRequest = Map.of("comment", "First comment");
        restTemplate.postForEntity("/api/incidents/" + incidentId + "/comments", commentRequest, Map.class);

        // List comments
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
                "/api/incidents/" + incidentId + "/comments",
                List.class
        );

        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertNotNull(listResponse.getBody());
        assertEquals(1, listResponse.getBody().size());
    }

    @Test
    void shouldAddInvestigation() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Investigation Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Add investigation
        Map<String, Object> investigationRequest = Map.of(
                "investigatorId", "investigator-123",
                "analysisMethod", "5 Whys",
                "rootCause", "Equipment maintenance was overdue",
                "findings", "Detailed investigation findings"
        );

        ResponseEntity<Map> investigationResponse = restTemplate.postForEntity(
                "/api/incidents/" + incidentId + "/investigation",
                investigationRequest,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, investigationResponse.getStatusCode());
        assertNotNull(investigationResponse.getBody());
        assertNotNull(investigationResponse.getBody().get("id"));
        assertEquals("investigator-123", investigationResponse.getBody().get("investigatorId"));
        assertEquals("5 Whys", investigationResponse.getBody().get("analysisMethod"));
        assertEquals("Equipment maintenance was overdue", investigationResponse.getBody().get("rootCause"));
        assertNotNull(investigationResponse.getBody().get("createdAt"));
    }

    @Test
    void shouldAddCorrectiveAction() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Corrective Action Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Add corrective action
        Map<String, Object> actionRequest = Map.of(
                "title", "Schedule equipment maintenance",
                "description", "Perform full maintenance check on machine",
                "assignedTo", "maintenance-user-789",
                "dueDate", "2026-03-20"
        );

        ResponseEntity<Map> actionResponse = restTemplate.postForEntity(
                "/api/incidents/" + incidentId + "/actions",
                actionRequest,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, actionResponse.getStatusCode());
        assertNotNull(actionResponse.getBody());
        assertNotNull(actionResponse.getBody().get("id"));
        assertEquals("Schedule equipment maintenance", actionResponse.getBody().get("title"));
        assertEquals("maintenance-user-789", actionResponse.getBody().get("assignedTo"));
        assertEquals("Pending", actionResponse.getBody().get("status"));
        assertNotNull(actionResponse.getBody().get("dueDate"));
    }

    @Test
    void shouldUpdateCorrectiveAction() {
        // Create incident
        Map<String, Object> createRequest = Map.of(
                "title", "Update Action Test",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/incidents", createRequest, Map.class);
        String incidentId = createResponse.getBody().get("id").toString();

        // Add corrective action
        Map<String, Object> actionRequest = Map.of(
                "title", "Test Action",
                "description", "Test action description"
        );

        ResponseEntity<Map> actionResponse = restTemplate.postForEntity(
                "/api/incidents/" + incidentId + "/actions",
                actionRequest,
                Map.class
        );

        Object actionId = actionResponse.getBody().get("id");

        // Update action status
        Map<String, Object> updateRequest = Map.of(
                "status", "Completed",
                "completedAt", OffsetDateTime.now().toString()
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateRequest);
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/incidents/actions/" + actionId,
                HttpMethod.PATCH,
                requestEntity,
                Map.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertNotNull(updateResponse.getBody());
        assertEquals("Completed", updateResponse.getBody().get("status"));
        assertNotNull(updateResponse.getBody().get("completedAt"));
    }

    @Test
    void shouldGetDashboardMetrics() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/incidents/dashboard", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify all required fields are present
        assertTrue(response.getBody().containsKey("totalIncidents"));
        assertTrue(response.getBody().containsKey("openIncidents"));
        assertTrue(response.getBody().containsKey("highSeverityIncidents"));
        assertTrue(response.getBody().containsKey("averageClosureTimeDays"));

        // Verify values are numeric
        assertInstanceOf(Number.class, response.getBody().get("totalIncidents"));
        assertInstanceOf(Number.class, response.getBody().get("openIncidents"));
        assertInstanceOf(Number.class, response.getBody().get("highSeverityIncidents"));
        assertInstanceOf(Number.class, response.getBody().get("averageClosureTimeDays"));
    }

    @Test
    void shouldReturn404ForNonExistentIncident() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/incidents/" + nonExistentId,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturn400ForInvalidIncidentCreation() {
        // Missing required fields
        Map<String, Object> invalidRequest = Map.of(
                "title", "Invalid Incident"
                // Missing typeId, severityId, occurredAt
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/incidents",
                invalidRequest,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldCreateMultipleIncidentsWithUniqueNumbers() {
        // Create first incident
        Map<String, Object> request1 = Map.of(
                "title", "First Incident",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> response1 = restTemplate.postForEntity("/api/incidents", request1, Map.class);
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        String incidentNumber1 = response1.getBody().get("incidentNumber").toString();

        // Create second incident of same type
        Map<String, Object> request2 = Map.of(
                "title", "Second Incident",
                "typeId", 1,
                "severityId", 1,
                "occurredAt", OffsetDateTime.now().toString()
        );

        ResponseEntity<Map> response2 = restTemplate.postForEntity("/api/incidents", request2, Map.class);
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        String incidentNumber2 = response2.getBody().get("incidentNumber").toString();

        // Verify incident numbers are unique
        assertNotEquals(incidentNumber1, incidentNumber2);

        // Verify both have same prefix and year but different sequence
        String[] parts1 = incidentNumber1.split("-");
        String[] parts2 = incidentNumber2.split("-");

        assertEquals(parts1[0], parts2[0]); // Same prefix
        assertEquals(parts1[1], parts2[1]); // Same year
        assertNotEquals(parts1[2], parts2[2]); // Different sequence
    }
}
