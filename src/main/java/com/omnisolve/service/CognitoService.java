package com.omnisolve.service;

import com.omnisolve.service.dto.CognitoUserResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CognitoService {

    private static final Logger log = LoggerFactory.getLogger(CognitoService.class);

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${app.security.cognito.user-pool-id}")
    private String userPoolId;

    public CognitoService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    /**
     * Create a new user in Cognito User Pool.
     *
     * @param email     User's email address (used as username)
     * @param firstName User's first name
     * @param lastName  User's last name
     * @return CognitoUserResult containing the username and sub (user ID)
     */
    public CognitoUserResult createUser(String email, String firstName, String lastName) {
        log.info("Creating Cognito user: email={}", email);
        
        try {
            AdminCreateUserRequest request = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .userAttributes(
                            AttributeType.builder()
                                    .name("email")
                                    .value(email)
                                    .build(),
                            AttributeType.builder()
                                    .name("email_verified")
                                    .value("true")
                                    .build(),
                            AttributeType.builder()
                                    .name("given_name")
                                    .value(firstName)
                                    .build(),
                            AttributeType.builder()
                                    .name("name")
                                    .value(firstName)
                                    .build(),
                            AttributeType.builder()
                                    .name("family_name")
                                    .value(lastName)
                                    .build()
                    )
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(request);
            String username = response.user().username();
            
            // Extract the sub from user attributes
            String sub = response.user().attributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .map(AttributeType::value)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Cognito user created but sub attribute not found"));
            
            log.info("Cognito user created successfully: username={}, sub={}, email={}", username, sub, email);
            log.info("Cognito invitation sent to {}", email);
            
            return new CognitoUserResult(username, sub);
            
        } catch (UsernameExistsException e) {
            log.warn("Cognito user already exists: email={}", email);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to create Cognito user: email={}, error={}", email, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("Failed to create user in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Disable a user in Cognito User Pool.
     *
     * @param username The Cognito username to disable
     */
    public void disableUser(String username) {
        log.info("Disabling Cognito user: username={}", username);
        
        try {
            AdminDisableUserRequest request = AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            cognitoClient.adminDisableUser(request);
            log.info("Cognito user disabled successfully: username={}", username);
            
        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to disable Cognito user: username={}, error={}", username, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("Failed to disable user in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Enable a user in Cognito User Pool.
     *
     * @param username The Cognito username to enable
     */
    public void enableUser(String username) {
        log.info("Enabling Cognito user: username={}", username);
        
        try {
            AdminEnableUserRequest request = AdminEnableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            cognitoClient.adminEnableUser(request);
            log.info("Cognito user enabled successfully: username={}", username);
            
        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to enable Cognito user: username={}, error={}", username, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("Failed to enable user in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Add a user to a Cognito group.
     *
     * @param username  The Cognito username
     * @param groupName The group name to add the user to
     */
    public void addUserToGroup(String username, String groupName) {
        log.info("Adding Cognito user to group: username={}, group={}", username, groupName);
        
        try {
            AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(groupName)
                    .build();

            cognitoClient.adminAddUserToGroup(request);
            log.info("Cognito user added to group successfully: username={}, group={}", username, groupName);
            
        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to add Cognito user to group: username={}, group={}, error={}", 
                    username, groupName, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("Failed to add user to group in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
