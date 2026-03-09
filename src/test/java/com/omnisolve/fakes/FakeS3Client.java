package com.omnisolve.fakes;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Fake S3Client implementation for integration tests.
 * Stores objects in memory without requiring AWS connectivity.
 * Uses dynamic proxy to avoid implementing all S3Client methods.
 */
public class FakeS3Client {

    private static final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    /**
     * Creates a fake S3Client that implements only the methods actually used.
     */
    public static S3Client create() {
        return (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                new S3ClientInvocationHandler()
        );
    }

    private static class S3ClientInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Handle Object methods
            if ("hashCode".equals(methodName)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            if ("toString".equals(methodName)) {
                return "FakeS3Client@" + Integer.toHexString(System.identityHashCode(proxy));
            }

            // Handle putObject with RequestBody
            if ("putObject".equals(methodName) && args != null && args.length == 2) {
                if (args[0] instanceof PutObjectRequest && args[1] instanceof RequestBody) {
                    return handlePutObject((PutObjectRequest) args[0], (RequestBody) args[1]);
                }
            }

            // Handle serviceName
            if ("serviceName".equals(methodName)) {
                return "s3";
            }

            // Handle close
            if ("close".equals(methodName)) {
                storage.clear();
                return null;
            }

            // All other methods throw UnsupportedOperationException
            throw new UnsupportedOperationException(
                    "Method " + methodName + " not implemented in FakeS3Client. " +
                    "Only putObject(PutObjectRequest, RequestBody) is supported."
            );
        }

        private PutObjectResponse handlePutObject(PutObjectRequest request, RequestBody body) {
            String key = request.bucket() + "/" + request.key();
            try {
                storage.put(key, body.contentStreamProvider().newStream().readAllBytes());
                return PutObjectResponse.builder()
                        .eTag("fake-etag-" + System.currentTimeMillis())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to store object in FakeS3Client", e);
            }
        }
    }

    /**
     * Get stored object for testing purposes.
     */
    public static byte[] getStoredObject(String bucket, String key) {
        return storage.get(bucket + "/" + key);
    }

    /**
     * Clear all stored objects.
     */
    public static void clearStorage() {
        storage.clear();
    }
}
