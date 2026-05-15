package com.jira.migration.storage;

import com.jira.migration.config.storage.AttachmentStorageConfig;
import com.jira.migration.config.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for obtaining the appropriate storage service based on configuration.
 */
@Component
@Slf4j
public class StorageFactory {

    private final AttachmentStorageConfig config;
    private final LocalStorageService localStorageService;
    private final S3StorageService s3StorageService;
    private final AzureBlobStorageService azureBlobStorageService;

    @Autowired
    public StorageFactory(
            AttachmentStorageConfig config,
            LocalStorageService localStorageService,
            S3StorageService s3StorageService,
            AzureBlobStorageService azureBlobStorageService) {
        this.config = config;
        this.localStorageService = localStorageService;
        this.s3StorageService = s3StorageService;
        this.azureBlobStorageService = azureBlobStorageService;

        log.info("Storage factory initialized with storage type: {}", config.getStorageType());
    }

    /**
     * Get the storage service based on the configured storage type.
     *
     * @return The appropriate storage service implementation
     */
    public AttachmentStorageService getStorageService() {
        return getStorageService(config.getStorageType());
    }

    /**
     * Get a specific storage service by type.
     *
     * @param type The storage type to use
     * @return The appropriate storage service implementation
     */
    public AttachmentStorageService getStorageService(StorageType type) {
        if (type == null) {
            type = StorageType.LOCAL;
        }

        return switch (type) {
            case LOCAL -> {
                log.debug("Using local storage service");
                yield localStorageService;
            }
            case S3 -> {
                log.debug("Using S3 storage service");
                yield s3StorageService;
            }
            case AZURE_BLOB -> {
                log.debug("Using Azure Blob storage service");
                yield azureBlobStorageService;
            }
        };
    }

    /**
     * Get the current storage type.
     */
    public StorageType getCurrentStorageType() {
        return config.getStorageType();
    }

    /**
     * Check if the storage is enabled.
     */
    public boolean isStorageEnabled() {
        return config.isEnabled();
    }

    /**
     * Get the configured bucket/container name.
     */
    public String getStorageLocation() {
        return switch (config.getStorageType()) {
            case LOCAL -> config.getBasePath();
            case S3 -> config.getS3().getBucket();
            case AZURE_BLOB -> config.getAzure().getContainer();
        };
    }
}