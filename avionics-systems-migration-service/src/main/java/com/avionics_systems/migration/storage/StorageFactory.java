package com.avionics_systems.migration.storage;

import com.avionics_systems.migration.config.storage.AttachmentStorageConfig;
import com.avionics_systems.migration.config.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Factory for obtaining the appropriate storage service based on configuration.
 */
@Component
@Slf4j
public class StorageFactory {

    private final AttachmentStorageConfig config;
    private final Optional<LocalStorageService> localStorageService;
    private final Optional<S3StorageService> s3StorageService;
    private final Optional<AzureBlobStorageService> azureBlobStorageService;

    @Autowired
    public StorageFactory(
            AttachmentStorageConfig config,
            Optional<LocalStorageService> localStorageService,
            Optional<S3StorageService> s3StorageService,
            Optional<AzureBlobStorageService> azureBlobStorageService) {
        this.config = config;
        this.localStorageService = localStorageService;
        this.s3StorageService = s3StorageService;
        this.azureBlobStorageService = azureBlobStorageService;

        log.info("Storage factory initialized with storage type: {}", config.getStorageType());
    }

    /**
     * Get the storage service based on the configured storage type.
     */
    public AttachmentStorageService getStorageService() {
        return getStorageService(config.getStorageType());
    }

    /**
     * Get a specific storage service by type.
     */
    public AttachmentStorageService getStorageService(StorageType type) {
        if (type == null) {
            type = StorageType.LOCAL;
        }

        return switch (type) {
            case LOCAL -> localStorageService.orElseThrow(() ->
                new IllegalStateException("Local storage service not available"));
            case S3 -> s3StorageService.orElseThrow(() ->
                new IllegalStateException("S3 storage service not available"));
            case AZURE_BLOB -> azureBlobStorageService.orElseThrow(() ->
                new IllegalStateException("Azure Blob storage service not available"));
        };
    }

    public StorageType getCurrentStorageType() {
        return config.getStorageType();
    }

    public boolean isStorageEnabled() {
        return config.isEnabled();
    }

    public String getStorageLocation() {
        return switch (config.getStorageType()) {
            case LOCAL -> config.getBasePath();
            case S3 -> config.getS3().getBucket();
            case AZURE_BLOB -> config.getAzure().getContainer();
        };
    }
}