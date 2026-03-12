package com.omnisolve.security;

import com.omnisolve.service.EmployeeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that activates employee accounts on their first successful login.
 * When an employee with "pending" status successfully authenticates,
 * their status is automatically updated to "active".
 */
@Component
public class FirstLoginFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirstLoginFilter.class);

    private final EmployeeService employeeService;

    public FirstLoginFilter(@Lazy EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Only process if user is authenticated with JWT
        if (authentication instanceof JwtAuthenticationToken jwtAuth && authentication.isAuthenticated()) {
            String cognitoSub = jwtAuth.getToken().getClaimAsString("sub");

            if (cognitoSub != null) {
                try {
                    employeeService.activateOnFirstLogin(cognitoSub);
                } catch (Exception e) {
                    // Log error but never block the request
                    log.error("Failed to activate employee on first login: cognitoSub={}, error={}",
                            cognitoSub, e.getMessage(), e);
                }
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
