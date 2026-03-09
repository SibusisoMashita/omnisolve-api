package com.omnisolve.config;

import com.omnisolve.fakes.FakeS3Client;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Test configuration that provides a fake S3Client for integration tests.
 * This avoids AWS connectivity and Mockito instrumentation issues with Java 25.
 */
@TestConfiguration
@Profile("test")
public class TestS3Config {

    @Bean
    @Primary
    public S3Client s3Client() {
        return FakeS3Client.create();
    }
}
