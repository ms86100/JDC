package com.avionics_systems.migration.config.storage;

/**
 * Enumeration of supported storage backend types.
 */
public enum StorageType {
    /**
     * Local filesystem storage (default).
     */
    LOCAL,

    /**
     * S3-compatible storage (AWS S3, MinIO, DigitalOcean Spaces, etc.).
     */
    S3,

    /**
     * Azure Blob Storage.
     */
    AZURE_BLOB
}