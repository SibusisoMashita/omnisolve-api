package com.omnisolve.config;

import com.omnisolve.fakes.FakeCognitoClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * Test configuration that replaces the real {@link CognitoIdentityProviderClient}
 * with a fake that returns stub responses without calling AWS.
 *
 * <p>Imported by {@link com.omnisolve.IntegrationTestBase} alongside
 * {@link TestS3Config} and {@link TestSecurityConfig}.
 */
@TestConfiguration
@Profile("test")
public class TestCognitoConfig {

    @Bean
    @Primary
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        return FakeCognitoClient.create();
    }
}
