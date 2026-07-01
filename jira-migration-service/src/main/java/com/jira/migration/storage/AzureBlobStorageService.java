package com.jira.migration.storage;

import com.jira.migration.config.storage.AttachmentStorageConfig;
import com.jira.migration.config.storage.StorageType;
import com.jira.migration.dto.attachment.AttachmentMetadata;
import com.jira.migration.dto.attachment.StoredAttachment;
import com.jira.migration.entity.attachment.AttachmentMetadataEntity;
import com.jira.migration.exception.StorageException;
import com.jira.migration.repository.attachment.AttachmentMetadataRepository;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.Map.Entry;

/**
 * Azure Blob Storage implementation for attachments.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "attachment.storage.storage-type", havingValue = "AZURE_BLOB")
public class AzureBlobStorageService implements AttachmentStorageService {

    private final AttachmentStorageConfig config;
    private final AttachmentMetadataRepository metadataRepository;
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private String containerName;

    @Autowired
    public AzureBlobStorageService(AttachmentStorageConfig config,
                                    AttachmentMetadataRepository metadataRepository) {
        this.config = config;
        this.metadataRepository = metadataRepository;
        this.containerName = config.getAzure().getContainer();
    }

    @PostConstruct
    public void init() {
        try {
            if (config.getAzure() == null || config.getAzure().getConnectionString() == null) {
                log.info("Azure Blob storage not configured, skipping initialization");
                this.blobServiceClient = null;
                this.containerClient = null;
                return;
            }

            String connectionString = config.getAzure().getConnectionString();

            this.blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            this.containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // Ensure container exists
            if (!containerClient.exists()) {
                containerClient.create();
                log.info("Created Azure Blob container: {}", containerName);
            }

            log.info("Azure Blob storage initialized for container: {}", containerName);

        } catch (Exception e) {
            log.warn("Failed to initialize Azure Blob storage: {}, will fall back to local storage", e.getMessage());
            this.blobServiceClient = null;
            this.containerClient = null;
        }
    }

    @Override
    public StoredAttachment store(String issueId, String fileName, InputStream data,
                                  long size, String mimeType, Map<String, String> metadata) {
        validateSize(size);

        String attachmentId = UUID.randomUUID().toString();
        String blobPath = generateBlobPath(issueId, fileName, attachmentId);

        try {
            // Read data into byte array for checksum calculation
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = data.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] content = baos.toByteArray();

            // Calculate checksum
            String checksum = calculateChecksum(content);

            // Get blob client
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            // Upload content
            blobClient.upload(BinaryData.fromBytes(content), true);

            // Set content type
            blobClient.setHttpHeaders(new BlobHttpHeaders()
                    .setContentType(mimeType));

            // Set metadata
            if (metadata != null && !metadata.isEmpty()) {
                Map<String, String> azureMetadata = convertMetadataToAzureFormat(metadata);
                blobClient.setMetadata(azureMetadata);
            }

            // Store metadata in database
            AttachmentMetadataEntity entity = AttachmentMetadataEntity.builder()
                    .id(attachmentId)
                    .issueId(issueId)
                    .fileName(fileName)
                    .originalFileName(fileName)
                    .mimeType(mimeType)
                    .sizeBytes(size)
                    .storagePath(blobPath)
                    .storageType(StorageType.AZURE_BLOB.name())
                    .checksum(checksum)
                    .contentHash(checksum)
                    .uploadedAt(Instant.now())
                    .virusScanStatus(AttachmentMetadataEntity.VirusScanStatus.NOT_SCANNED)
                    .metadataJson(metadata != null ? metadata : Map.of())
                    .build();

            metadataRepository.save(entity);

            log.info("Stored attachment {} in Azure Blob for issue {}: {} ({} bytes)",
                    attachmentId, issueId, fileName, size);

            return buildStoredAttachment(entity);

        } catch (Exception e) {
            log.error("Azure error storing attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to store attachment in Azure Blob: " + e.getMessage(),
                    attachmentId, com.jira.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.AZURE_BLOB, e);
        }
    }

    @Override
    public InputStream retrieve(String attachmentId) {
        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        if (entity.isDeleted()) {
            throw StorageException.notFound(attachmentId);
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(entity.getStoragePath());

            if (!blobClient.exists()) {
                log.error("Azure blob not found for attachment {}: {}", attachmentId, entity.getStoragePath());
                throw StorageException.notFound(attachmentId);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);

            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Azure error retrieving attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to retrieve attachment from Azure Blob",
                    attachmentId, com.jira.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.AZURE_BLOB, e);
        }
    }

    @Override
    public AttachmentMetadata getMetadata(String attachmentId) {
        return metadataRepository.findById(attachmentId)
                .filter(e -> !e.isDeleted())
                .map(AttachmentMetadataEntity::toDto)
                .orElse(null);
    }

    @Override
    public void delete(String attachmentId) {
        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        try {
            // Delete from Azure Blob
            BlobClient blobClient = containerClient.getBlobClient(entity.getStoragePath());
            blobClient.delete();

            // Soft delete metadata
            entity.setDeleted(true);
            entity.setDeletedAt(Instant.now());
            metadataRepository.save(entity);

            log.info("Deleted attachment {} from Azure Blob: {}", attachmentId, entity.getFileName());

        } catch (Exception e) {
            log.error("Azure error deleting attachment {}: {}", attachmentId, e.getMessage(), e);
            // Still soft delete the metadata
            entity.setDeleted(true);
            entity.setDeletedAt(Instant.now());
            metadataRepository.save(entity);
        }
    }

    @Override
    public boolean exists(String attachmentId) {
        return metadataRepository.findById(attachmentId)
                .filter(e -> !e.isDeleted())
                .map(e -> {
                    try {
                        BlobClient blobClient = containerClient.getBlobClient(e.getStoragePath());
                        return blobClient.exists();
                    } catch (Exception ex) {
                        log.error("Error checking if blob exists for {}: {}", attachmentId, ex.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }

    @Override
    public String getDownloadUrl(String attachmentId, Duration validity) {
        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        try {
            BlobClient blobClient = containerClient.getBlobClient(entity.getStoragePath());

            // Return the blob URL - in production, add SAS token for secure access
            return blobClient.getBlobUrl();

        } catch (Exception e) {
            log.error("Failed to generate download URL for {}: {}", attachmentId, e.getMessage());
            throw new StorageException("Failed to generate download URL",
                    attachmentId, com.jira.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.AZURE_BLOB, e);
        }
    }

    @Override
    public String copy(String sourceAttachmentId, String targetIssueId) {
        AttachmentMetadataEntity source = metadataRepository.findById(sourceAttachmentId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> StorageException.notFound(sourceAttachmentId));

        String newFileName = generateCopyFileName(source.getFileName());
        String newAttachmentId = UUID.randomUUID().toString();
        String newBlobPath = generateBlobPath(targetIssueId, newFileName, newAttachmentId);

        try {
            // Copy blob in Azure
            BlobClient sourceBlob = containerClient.getBlobClient(source.getStoragePath());
            BlobClient destBlob = containerClient.getBlobClient(newBlobPath);

            destBlob.copyFromUrl(sourceBlob.getBlobUrl());

            // Create new metadata entry
            AttachmentMetadataEntity newEntity = AttachmentMetadataEntity.builder()
                    .id(newAttachmentId)
                    .issueId(targetIssueId)
                    .fileName(newFileName)
                    .originalFileName(source.getOriginalFileName())
                    .mimeType(source.getMimeType())
                    .sizeBytes(source.getSizeBytes())
                    .storagePath(newBlobPath)
                    .storageType(StorageType.AZURE_BLOB.name())
                    .checksum(source.getChecksum())
                    .contentHash(source.getContentHash())
                    .uploadedAt(Instant.now())
                    .virusScanStatus(source.getVirusScanStatus())
                    .metadataJson(source.getMetadataJson())
                    .build();

            metadataRepository.save(newEntity);

            log.info("Copied attachment {} to {} for issue {}",
                    sourceAttachmentId, newAttachmentId, targetIssueId);

            return newAttachmentId;

        } catch (Exception e) {
            log.error("Failed to copy attachment {} to Azure Blob: {}", sourceAttachmentId, e.getMessage());
            throw new StorageException("Failed to copy attachment",
                    sourceAttachmentId, com.jira.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.AZURE_BLOB, e);
        }
    }

    @Override
    public List<StoredAttachment> storeBatch(List<AttachmentUploadRequest> requests) {
        List<StoredAttachment> results = new ArrayList<>();

        for (AttachmentUploadRequest request : requests) {
            try {
                StoredAttachment result = store(
                        request.getIssueId(),
                        request.getFileName(),
                        request.getData(),
                        request.getSize(),
                        request.getMimeType(),
                        request.getMetadata()
                );
                results.add(result);
            } catch (Exception e) {
                log.error("Batch store failed for file {}: {}",
                        request.getFileName(), e.getMessage());
            }
        }

        return results;
    }

    @Override
    public long getTotalSizeForIssue(String issueId) {
        return metadataRepository.sumSizeBytesByIssueId(issueId);
    }

    @Override
    public int getAttachmentCountForIssue(String issueId) {
        return (int) metadataRepository.countByIssueIdAndDeletedFalse(issueId);
    }

    // Helper methods

    private String generateBlobPath(String issueId, String fileName, String attachmentId) {
        YearMonth yearMonth = YearMonth.now();
        String sanitizedFileName = sanitizeFileName(fileName);

        return String.format("%d/%02d/%s/%s/%s",
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                issueId,
                attachmentId,
                sanitizedFileName);
    }

    private String generateCopyFileName(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String name = originalFileName.substring(0, dotIndex);
            String ext = originalFileName.substring(dotIndex);
            return name + "_copy" + ext;
        }
        return originalFileName + "_copy";
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Map<String, String> convertMetadataToAzureFormat(Map<String, String> metadata) {
        Map<String, String> result = new HashMap<>();
        for (Entry<String, String> entry : metadata.entrySet()) {
            result.put(" attachment-" + entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void validateSize(long size) {
        long maxSize = config.getMaxFileSizeBytes();
        if (size > maxSize) {
            throw new StorageException(
                    String.format("File size %d exceeds maximum allowed %d bytes", size, maxSize),
                    null,
                    com.jira.migration.exception.StorageErrorType.QUOTA_EXCEEDED);
        }
    }

    private String calculateChecksum(byte[] content) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    private StoredAttachment buildStoredAttachment(AttachmentMetadataEntity entity) {
        return StoredAttachment.builder()
                .attachmentId(entity.getId())
                .issueId(entity.getIssueId())
                .fileName(entity.getFileName())
                .storagePath(entity.getStoragePath())
                .storageType(entity.getStorageType())
                .sizeBytes(entity.getSizeBytes())
                .mimeType(entity.getMimeType())
                .checksum(entity.getChecksum())
                .contentHash(entity.getContentHash())
                .uploadedAt(entity.getUploadedAt())
                .downloadUrl(getDownloadUrl(entity.getId(), Duration.ofHours(1)))
                .metadata(entity.getMetadataJson())
                .build();
    }
}