package com.omnisolve.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class AwsS3StorageService implements S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(AwsS3StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public AwsS3StorageService(S3Client s3Client, @Value("${app.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public String upload(byte[] payload, String key, String contentType) {
        log.info("S3 upload starting: bucket={}, key={}, contentType={}, payloadSize={}", 
                bucketName, key, contentType, payload.length);
        
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(payload));
            log.info("S3 upload completed successfully: bucket={}, key={}", bucketName, key);
            return key;
        } catch (Exception e) {
            log.error("S3 upload failed: bucket={}, key={}, error={}", 
                    bucketName, key, e.getMessage(), e);
            throw e;
        }
    }
}

