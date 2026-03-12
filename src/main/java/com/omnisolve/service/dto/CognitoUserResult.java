package com.omnisolve.service.dto;

/**
 * Result of creating a user in Cognito.
 * Contains both the username and the unique sub (user ID).
 */
public record CognitoUserResult(
        String username,
        String sub
) {
}
