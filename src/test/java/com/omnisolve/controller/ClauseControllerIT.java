package com.omnisolve.controller;

import com.omnisolve.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ClauseControllerIT extends IntegrationTestBase {

    @Test
    void shouldListClauses() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/clauses", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateClause() {
        String uniqueCode = "4.1." + System.currentTimeMillis();
        Map<String, Object> request = Map.of(
                "code", uniqueCode,
                "title", "Understanding the organization and its context",
                "description", "The organization shall determine external and internal issues"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/clauses", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals(uniqueCode, response.getBody().get("code"));
        assertEquals("Understanding the organization and its context", response.getBody().get("title"));
    }
}
