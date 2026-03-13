package com.omnisolve.event;

import java.util.UUID;

/**
 * Published when a new document version file is successfully uploaded to S3.
 *
 * @param documentId     the document UUID
 * @param documentNumber human-readable document number
 * @param versionNumber  the new version number that was just created
 * @param s3Key          S3 object key of the uploaded file
 * @param fileName       original file name as submitted by the client
 * @param organisationId the tenant the document belongs to
 * @param uploadedBy     Cognito sub of the user who uploaded the file
 */
public record DocumentUploadedEvent(
        UUID documentId,
        String documentNumber,
        int versionNumber,
        String s3Key,
        String fileName,
        Long organisationId,
        String uploadedBy) {
}
