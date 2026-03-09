package com.omnisolve.controller;

import com.omnisolve.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DocumentControllerIT extends IntegrationTestBase {

    @Test
    void shouldListDocuments() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/documents", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateDocument() {
        String uniqueDocNumber = "DOC-" + System.currentTimeMillis();
        Map<String, Object> request = Map.of(
                "documentNumber", uniqueDocNumber,
                "title", "Quality Policy",
                "typeId", 1,
                "departmentId", 1,
                "ownerId", "test-user"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/documents", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals("Quality Policy", response.getBody().get("title"));
    }

    @Test
    void shouldGetDocumentById() {
        // First create a document
        String uniqueDocNumber = "DOC-" + System.currentTimeMillis();
        Map<String, Object> createRequest = Map.of(
                "documentNumber", uniqueDocNumber,
                "title", "Test Document",
                "typeId", 1,
                "departmentId", 1,
                "ownerId", "test-user"
        );

        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/documents", createRequest, Map.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        String documentId = createResponse.getBody().get("id").toString();

        // Then retrieve it
        ResponseEntity<Map> getResponse = restTemplate.getForEntity("/api/documents/" + documentId, Map.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(documentId, getResponse.getBody().get("id"));
        assertEquals("Test Document", getResponse.getBody().get("title"));
    }
}
