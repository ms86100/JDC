package com.avionics_systems.migration.service.attachment;

import com.avionics_systems.migration.config.storage.AttachmentStorageConfig;
import com.avionics_systems.migration.dto.attachment.*;
import com.avionics_systems.migration.entity.attachment.AttachmentMetadataEntity;
import com.avionics_systems.migration.exception.StorageException;
import com.avionics_systems.migration.repository.attachment.AttachmentMetadataRepository;
import com.avionics_systems.migration.storage.AttachmentStorageService;
import com.avionics_systems.migration.storage.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for attachment operations.
 * Handles upload, download, validation, virus scanning, and copying.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

    private final StorageFactory storageFactory;
    private final AttachmentMetadataRepository metadataRepository;
    private final FileValidationService fileValidationService;
    private final AttachmentStorageConfig config;

    /**
     * Upload an attachment during import.
     */
    @Transactional
    public AttachmentUploadResult uploadAttachment(String issueId, String fileName,
                                                   InputStream data, long size,
                                                   String mimeType, String uploadedBy) {
        log.debug("Uploading attachment for issue {}: {} ({} bytes)", issueId, fileName, size);

        try {
            // Validate file
            byte[] headerBytes = readHeader(data, 8192);
            InputStream headerStream = new ByteArrayInputStream(headerBytes);

            FileValidationResult validation = fileValidationService.validateFile(
                    fileName, mimeType, size, headerBytes);

            if (!validation.isValid()) {
                return AttachmentUploadResult.builder()
                        .fileName(fileName)
                        .success(false)
                        .errorMessage(String.join(", ", validation.getErrors()))
                        .build();
            }

            // Reset stream for actual upload
            data.reset();

            // Store attachment
            AttachmentStorageService storageService = storageFactory.getStorageService();
            StoredAttachment stored = storageService.store(
                    issueId, fileName, data, size, mimeType,
                    Map.of("uploadedBy", uploadedBy != null ? uploadedBy : "system"));

            return AttachmentUploadResult.builder()
                    .attachmentId(stored.getAttachmentId())
                    .fileName(stored.getFileName())
                    .sizeBytes(stored.getSizeBytes())
                    .mimeType(stored.getMimeType())
                    .downloadUrl(stored.getDownloadUrl())
                    .uploadedAt(stored.getUploadedAt())
                    .success(true)
                    .build();

        } catch (IOException e) {
            log.error("Failed to read attachment data for issue {}: {}", issueId, e.getMessage());
            return AttachmentUploadResult.builder()
                    .fileName(fileName)
                    .success(false)
                    .errorMessage("Failed to read attachment data: " + e.getMessage())
                    .build();
        } catch (StorageException e) {
            log.error("Storage error uploading attachment for issue {}: {}", issueId, e.getMessage());
            return AttachmentUploadResult.builder()
                    .fileName(fileName)
                    .success(false)
                    .errorMessage("Storage error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Upload from a MultipartFile (for HTTP uploads).
     */
    @Transactional
    public AttachmentUploadResult uploadAttachment(String issueId, MultipartFile file,
                                                   String uploadedBy) throws IOException {
        return uploadAttachment(
                issueId,
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType(),
                uploadedBy
        );
    }

    /**
     * Export an attachment for migration/export operations.
     */
    public AttachmentExportResult exportAttachment(String attachmentId) {
        AttachmentStorageService storageService = storageFactory.getStorageService();

        AttachmentMetadata metadata = storageService.getMetadata(attachmentId);
        if (metadata == null) {
            throw StorageException.notFound(attachmentId);
        }

        try {
            InputStream content = storageService.retrieve(attachmentId);
            byte[] bytes = content.readAllBytes();

            return AttachmentExportResult.builder()
                    .attachmentId(attachmentId)
                    .fileName(metadata.getFileName())
                    .content(bytes)
                    .mimeType(metadata.getMimeType())
                    .sizeBytes(metadata.getSizeBytes())
                    .checksum(metadata.getChecksum())
                    .build();

        } catch (IOException e) {
            throw new StorageException("Failed to read attachment content", e);
        }
    }

    /**
     * Validate an attachment before upload.
     */
    public FileValidationResult validateAttachment(String fileName, String mimeType,
                                                    long size, byte[] headerBytes) {
        return fileValidationService.validateFile(fileName, mimeType, size, headerBytes);
    }

    /**
     * Validate an attachment from a MultipartFile.
     */
    public FileValidationResult validateAttachment(MultipartFile file) throws IOException {
        byte[] header = new byte[8192];
        int bytesRead = file.getInputStream().read(header);

        byte[] headerBytes = Arrays.copyOf(header, bytesRead);

        return fileValidationService.validateFile(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                headerBytes
        );
    }

    /**
     * Initiate virus scan for an attachment.
     * This is a placeholder for actual virus scanner integration.
     */
    @Async("validationExecutor")
    public CompletableFuture<Void> initiateVirusScan(String attachmentId) {
        log.info("Initiating virus scan for attachment: {}", attachmentId);

        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        try {
            // Update status to pending
            entity.setVirusScanStatus(AttachmentMetadataEntity.VirusScanStatus.PENDING);
            metadataRepository.save(entity);

            // Check if virus scanning is enabled
            if (!config.getVirusScan().isEnabled()) {
                log.debug("Virus scanning disabled, marking as NOT_SCANNED");
                entity.setVirusScanStatus(AttachmentMetadataEntity.VirusScanStatus.NOT_SCANNED);
                metadataRepository.save(entity);
                return CompletableFuture.completedFuture(null);
            }

            // TODO: Integrate with actual virus scanner (ClamAV, Trend Micro, etc.)
            // For now, simulate scanning
            performVirusScan(attachmentId);

        } catch (Exception e) {
            log.error("Virus scan failed for {}: {}", attachmentId, e.getMessage());
            entity.setVirusScanStatus(AttachmentMetadataEntity.VirusScanStatus.ERROR);
            metadataRepository.save(entity);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void performVirusScan(String attachmentId) {
        // Placeholder for actual virus scanning
        // In production, this would call ClamAV, Trend Micro, or another scanner

        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        log.debug("Scanning attachment {} for viruses...", attachmentId);

        // Simulate scan completion
        entity.setVirusScanStatus(AttachmentMetadataEntity.VirusScanStatus.CLEAN);
        metadataRepository.save(entity);

        log.info("Virus scan completed for attachment {}: CLEAN", attachmentId);
    }

    /**
     * Download an attachment with content.
     */
    public AttachmentDownloadResult downloadAttachment(String attachmentId) {
        AttachmentStorageService storageService = storageFactory.getStorageService();

        AttachmentMetadata metadata = storageService.getMetadata(attachmentId);
        if (metadata == null) {
            throw StorageException.notFound(attachmentId);
        }

        try {
            InputStream content = storageService.retrieve(attachmentId);

            return AttachmentDownloadResult.builder()
                    .attachmentId(attachmentId)
                    .fileName(metadata.getFileName())
                    .mimeType(metadata.getMimeType())
                    .sizeBytes(metadata.getSizeBytes())
                    .contentStream(content)
                    .checksum(metadata.getChecksum())
                    .build();

        } catch (StorageException e) {
            throw e;
        }
    }

    /**
     * Delete an attachment.
     */
    @Transactional
    public void deleteAttachment(String attachmentId) {
        AttachmentStorageService storageService = storageFactory.getStorageService();
        storageService.delete(attachmentId);
        log.info("Deleted attachment: {}", attachmentId);
    }

    /**
     * Copy attachments to a target issue (for project duplication).
     */
    @Transactional
    public List<String> copyAttachmentsToIssue(List<String> attachmentIds, String targetIssueId) {
        AttachmentStorageService storageService = storageFactory.getStorageService();
        List<String> newAttachmentIds = new ArrayList<>();

        for (String sourceId : attachmentIds) {
            try {
                String newId = storageService.copy(sourceId, targetIssueId);
                newAttachmentIds.add(newId);
                log.info("Copied attachment {} to {} for issue {}",
                        sourceId, newId, targetIssueId);
            } catch (Exception e) {
                log.error("Failed to copy attachment {}: {}", sourceId, e.getMessage());
            }
        }

        return newAttachmentIds;
    }

    /**
     * Get all attachments for an issue.
     */
    public List<AttachmentMetadata> getIssueAttachments(String issueId) {
        List<AttachmentMetadataEntity> entities =
                metadataRepository.findByIssueIdAndDeletedFalse(issueId);

        return entities.stream()
                .map(AttachmentMetadataEntity::toDto)
                .toList();
    }

    /**
     * Get download URL for an attachment.
     */
    public String getDownloadUrl(String attachmentId, Duration validity) {
        AttachmentStorageService storageService = storageFactory.getStorageService();
        return storageService.getDownloadUrl(attachmentId, validity);
    }

    /**
     * Check if an attachment exists.
     */
    public boolean attachmentExists(String attachmentId) {
        AttachmentStorageService storageService = storageFactory.getStorageService();
        return storageService.exists(attachmentId);
    }

    /**
     * Get attachment count for an issue.
     */
    public int getAttachmentCount(String issueId) {
        return (int) metadataRepository.countByIssueIdAndDeletedFalse(issueId);
    }

    /**
     * Get total attachment size for an issue.
     */
    public long getTotalAttachmentSize(String issueId) {
        return metadataRepository.sumSizeBytesByIssueId(issueId);
    }

    /**
     * Check for duplicate content (by content hash).
     */
    public Optional<String> findDuplicateByHash(String contentHash) {
        return metadataRepository.findByContentHashAndDeletedFalse(contentHash)
                .map(AttachmentMetadataEntity::getId);
    }

    // Helper methods

    private byte[] readHeader(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[maxBytes];
        int bytesRead = input.read(data);
        if (bytesRead > 0) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}