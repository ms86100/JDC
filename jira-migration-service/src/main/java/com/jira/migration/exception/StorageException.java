package com.jira.migration.exception;

import com.jira.migration.config.storage.StorageType;
import lombok.Getter;

/**
 * Exception thrown when storage operations fail.
 */
@Getter
public class StorageException extends RuntimeException {

    private final String attachmentId;
    private final StorageErrorType errorType;
    private final StorageType storageType;

    public StorageException(String message) {
        super(message);
        this.attachmentId = null;
        this.errorType = StorageErrorType.STORAGE_ERROR;
        this.storageType = null;
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.attachmentId = null;
        this.errorType = StorageErrorType.STORAGE_ERROR;
        this.storageType = null;
    }

    public StorageException(String message, String attachmentId, StorageErrorType errorType) {
        super(message);
        this.attachmentId = attachmentId;
        this.errorType = errorType;
        this.storageType = null;
    }

    public StorageException(String message, String attachmentId, StorageErrorType errorType,
                           StorageType storageType, Throwable cause) {
        super(message, cause);
        this.attachmentId = attachmentId;
        this.errorType = errorType;
        this.storageType = storageType;
    }

    public StorageException(String message, String attachmentId, StorageErrorType errorType,
                           Throwable cause) {
        super(message, cause);
        this.attachmentId = attachmentId;
        this.errorType = errorType;
        this.storageType = null;
    }

    /**
     * Create a NOT_FOUND exception.
     */
    public static StorageException notFound(String attachmentId) {
        return new StorageException(
                "Attachment not found: " + attachmentId,
                attachmentId,
                StorageErrorType.NOT_FOUND
        );
    }

    /**
     * Create an ACCESS_DENIED exception.
     */
    public static StorageException accessDenied(String attachmentId) {
        return new StorageException(
                "Access denied to attachment: " + attachmentId,
                attachmentId,
                StorageErrorType.ACCESS_DENIED
        );
    }

    /**
     * Create a QUOTA_EXCEEDED exception.
     */
    public static StorageException quotaExceeded(long usedBytes, long limitBytes) {
        return new StorageException(
                String.format("Storage quota exceeded: used %d bytes, limit %d bytes", usedBytes, limitBytes),
                null,
                StorageErrorType.QUOTA_EXCEEDED
        );
    }

    /**
     * Create an INVALID_FILE exception.
     */
    public static StorageException invalidFile(String attachmentId, String reason) {
        return new StorageException(
                "Invalid file: " + reason,
                attachmentId,
                StorageErrorType.INVALID_FILE
        );
    }

    /**
     * Create a VIRUS_DETECTED exception.
     */
    public static StorageException virusDetected(String attachmentId) {
        return new StorageException(
                "Virus detected in attachment: " + attachmentId,
                attachmentId,
                StorageErrorType.VIRUS_DETECTED
        );
    }

    /**
     * Create a STORAGE_ERROR exception.
     */
    public static StorageException storageError(String message, Throwable cause) {
        return new StorageException(message, cause);
    }
}