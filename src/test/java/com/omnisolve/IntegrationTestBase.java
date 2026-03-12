package com.omnisolve;

import com.omnisolve.config.TestCognitoConfig;
import com.omnisolve.config.TestS3Config;
import com.omnisolve.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses embedded PostgreSQL (no Docker required).
 * Tests run with full Spring Boot context and real database.
 * Includes mock authentication for multi-tenant testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestS3Config.class, TestCognitoConfig.class, TestSecurityConfig.class})
public abstract class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeEach
    void configurePatchCapableClient() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }
}
