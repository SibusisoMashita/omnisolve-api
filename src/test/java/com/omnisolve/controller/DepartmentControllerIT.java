package com.omnisolve.controller;

import com.omnisolve.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DepartmentControllerIT extends IntegrationTestBase {

    @Test
    void shouldListDepartments() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/departments", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateDepartment() {
        String uniqueName = "Test Department " + System.currentTimeMillis();
        Map<String, Object> request = Map.of(
                "name", uniqueName,
                "description", "A test department"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/departments", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals(uniqueName, response.getBody().get("name"));
        assertEquals("A test department", response.getBody().get("description"));
    }
}
