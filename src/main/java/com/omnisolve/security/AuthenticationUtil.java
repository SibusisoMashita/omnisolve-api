package com.omnisolve.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Utility class for extracting authenticated user information from JWT tokens.
 */
public class AuthenticationUtil {

    private AuthenticationUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the authenticated user ID from the JWT token's 'sub' claim.
     * Falls back to "system" if no authentication is present (e.g., in development mode).
     *
     * @return the user ID from the JWT 'sub' claim, or "system" if not authenticated
     */
    public static String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getClaimAsString("sub");
            return sub != null ? sub : "system";
        }

        // Fallback for other authentication types
        return authentication.getName() != null ? authentication.getName() : "system";
    }

    /**
     * Get the authenticated user's email from the JWT token's 'email' claim.
     *
     * @return the user's email, or null if not available
     */
    public static String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("email");
        }

        return null;
    }

    /**
     * Get the authenticated user's username from the JWT token's 'cognito:username' claim.
     *
     * @return the username, or null if not available
     */
    public static String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("cognito:username");
        }

        return null;
    }

    /**
     * Get a specific claim from the JWT token.
     *
     * @param claimName the name of the claim to retrieve
     * @return the claim value as a String, or null if not available
     */
    public static String getClaim(String claimName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString(claimName);
        }

        return null;
    }

    /**
     * Get the full JWT token.
     *
     * @return the JWT token, or null if not available
     */
    public static Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }

        return null;
    }

    /**
     * Check if the current request is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !(authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal()));
    }
}
