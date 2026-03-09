package com.omnisolve.controller;

import com.omnisolve.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DocumentTypeControllerIT extends IntegrationTestBase {

    @Test
    void shouldListDocumentTypes() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/document-types", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateDocumentType() {
        String uniqueName = "Test Document Type " + System.currentTimeMillis();
        Map<String, Object> request = Map.of(
                "name", uniqueName,
                "description", "A test document type"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/document-types", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals(uniqueName, response.getBody().get("name"));
        assertEquals("A test document type", response.getBody().get("description"));
    }
}
