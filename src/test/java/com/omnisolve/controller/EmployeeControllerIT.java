package com.omnisolve.controller;

import com.omnisolve.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class EmployeeControllerIT extends IntegrationTestBase {

    @Test
    void shouldListEmployees() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/employees", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test@omnisolve.test"));
    }

    @Test
    void shouldCreateEmployee() {
        String uniqueEmail = "employee-" + System.currentTimeMillis() + "@omnisolve.test";
        Map<String, Object> request = Map.of(
                "cognitoSub", "test-sub-" + System.currentTimeMillis(),
                "cognitoUsername", "testemployee" + System.currentTimeMillis(),
                "email", uniqueEmail,
                "firstName", "Test",
                "lastName", "Employee",
                "role", "user"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/employees", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals("Test", response.getBody().get("firstName"));
        assertEquals("Employee", response.getBody().get("lastName"));
        assertEquals(uniqueEmail, response.getBody().get("email"));
    }
}
