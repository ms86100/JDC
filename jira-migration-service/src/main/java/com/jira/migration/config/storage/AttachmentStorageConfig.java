package com.jira.migration.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for attachment storage.
 * Supports local filesystem, S3-compatible, and Azure Blob storage backends.
 */
@Data
@Component
@ConfigurationProperties(prefix = "attachment.storage")
public class AttachmentStorageConfig {

    /**
     * Whether attachment storage is enabled.
     */
    private boolean enabled = true;

    /**
     * The storage backend type to use.
     */
    private StorageType storageType = StorageType.LOCAL;

    /**
     * Base path for local storage.
     */
    private String basePath = "/var/attachments";

    /**
     * Maximum size for a single file in megabytes.
     */
    private long maxFileSizeMb = 50;

    /**
     * Maximum total storage size in megabytes.
     */
    private long maxTotalSizeMb = 500;

    /**
     * S3 configuration.
     */
    private S3Config s3 = new S3Config();

    /**
     * Azure Blob Storage configuration.
     */
    private AzureConfig azure = new AzureConfig();

    /**
     * Virus scanning configuration.
     */
    private VirusScanConfig virusScan = new VirusScanConfig();

    /**
     * Allowed MIME types for uploads.
     */
    private List<String> allowedMimeTypes = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
            "application/pdf",
            "application/zip",
            "application/x-zip-compressed",
            "application/x-7z-compressed",
            "application/x-rar-compressed",
            "text/plain",
            "text/csv",
            "application/json",
            "application/xml",
            "text/xml",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    @Data
    public static class S3Config {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket = "attachments";
        private String region = "us-east-1";
        private boolean pathStyleAccess = true;
    }

    @Data
    public static class AzureConfig {
        private String connectionString;
        private String container = "attachments";
    }

    @Data
    public static class VirusScanConfig {
        private boolean enabled = false;
        private String provider = "NONE"; // NONE, CLAMAV, TRENDMICRO
        private boolean scanBeforeImport = true;
    }

    /**
     * Get the maximum file size in bytes.
     */
    public long getMaxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }

    /**
     * Get the maximum total storage size in bytes.
     */
    public long getMaxTotalSizeBytes() {
        return maxTotalSizeMb * 1024 * 1024;
    }

    /**
     * Check if a MIME type is allowed.
     */
    public boolean isAllowedMimeType(String mimeType) {
        return allowedMimeTypes.contains(mimeType);
    }
}