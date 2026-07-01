package com.jira.migration.exception;

/**
 * Types of storage errors that can occur.
 */
public enum StorageErrorType {
    /**
     * The requested attachment was not found.
     */
    NOT_FOUND,

    /**
     * Access to the attachment was denied.
     */
    ACCESS_DENIED,

    /**
     * Storage quota has been exceeded.
     */
    QUOTA_EXCEEDED,

    /**
     * The file is invalid or corrupted.
     */
    INVALID_FILE,

    /**
     * A virus was detected in the file.
     */
    VIRUS_DETECTED,

    /**
     * A general storage error occurred.
     */
    STORAGE_ERROR
}