package com.omnisolve.config;

import com.omnisolve.domain.Employee;
import com.omnisolve.domain.Organisation;
import com.omnisolve.repository.EmployeeRepository;
import com.omnisolve.repository.OrganisationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Test configuration that provides mock authentication for integration tests.
 * Creates a test employee linked to the demo organisation so that multi-tenant
 * queries work correctly in tests.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    public static final String TEST_USER_SUB = "test-user-123";
    public static final String TEST_USER_EMAIL = "test@omnisolve.test";
    public static final String TEST_USER_USERNAME = "testuser";

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Initialize test data after Spring context is loaded.
     * Creates a test employee linked to the demo organisation.
     */
    @PostConstruct
    public void initTestData() {
        // Find or create demo organisation
        Organisation demoOrg = organisationRepository.findAll().stream()
                .filter(org -> org.getName().contains("Demo"))
                .findFirst()
                .orElseGet(() -> {
                    Organisation org = new Organisation();
                    org.setName("OmniSolve Demo Organisation");
                    return organisationRepository.save(org);
                });

        // Create test employee if not exists
        if (employeeRepository.findByCognitoSub(TEST_USER_SUB).isEmpty()) {
            Employee testEmployee = new Employee();
            testEmployee.setCognitoSub(TEST_USER_SUB);
            testEmployee.setCognitoUsername(TEST_USER_USERNAME);
            testEmployee.setEmail(TEST_USER_EMAIL);
            testEmployee.setFirstName("Test");
            testEmployee.setLastName("User");
            testEmployee.setOrganisation(demoOrg);
            testEmployee.setStatus("active");
            testEmployee.setCreatedAt(java.time.OffsetDateTime.now());
            testEmployee.setUpdatedAt(java.time.OffsetDateTime.now());
            employeeRepository.save(testEmployee);
        }
    }

    /**
     * Register a filter that sets up mock authentication for each request.
     * This ensures that AuthenticationUtil.getAuthenticatedUserId() works in tests.
     */
    @Bean
    public FilterRegistrationBean<TestAuthenticationFilter> testAuthenticationFilter() {
        FilterRegistrationBean<TestAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TestAuthenticationFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    /**
     * Filter that sets up mock authentication for each request in tests.
     */
    public static class TestAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            
            // Set up mock authentication for this request
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    TEST_USER_SUB,
                    null,
                    java.util.Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clean up after request
                SecurityContextHolder.clearContext();
            }
        }
    }
}
