package com.omnisolve.service;

public interface S3StorageService {

    String upload(byte[] payload, String key, String contentType);
}

