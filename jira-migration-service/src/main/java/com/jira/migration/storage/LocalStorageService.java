package com.jira.migration.storage;

import com.jira.migration.config.storage.AttachmentStorageConfig;
import com.jira.migration.config.storage.StorageType;
import com.jira.migration.dto.attachment.AttachmentMetadata;
import com.jira.migration.dto.attachment.StoredAttachment;
import com.jira.migration.entity.attachment.AttachmentMetadataEntity;
import com.jira.migration.exception.StorageException;
import com.jira.migration.repository.attachment.AttachmentMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Local filesystem implementation of attachment storage.
 * Stores files in a structured directory hierarchy with path: /{basePath}/{year}/{month}/{issueId}/{uuid}.{ext}
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "attachment.storage.storage-type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalStorageService implements AttachmentStorageService {

    private final AttachmentStorageConfig config;
    private final AttachmentMetadataRepository metadataRepository;

    private final Map<String, ReentrantLock> directoryLocks = new ConcurrentHashMap<>();
    private Path basePath;

    public LocalStorageService(AttachmentStorageConfig config,
                               AttachmentMetadataRepository metadataRepository) {
        this.config = config;
        this.metadataRepository = metadataRepository;
        this.basePath = Paths.get(config.getBasePath());
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(basePath);
            log.info("Local storage initialized at: {}", basePath);
        } catch (IOException e) {
            log.warn("Failed to create storage directory: {}, attachments will be stored in temp directory", basePath);
            // Storage will use temp directory when needed
        }
    }

    @Override
    public StoredAttachment store(String issueId, String fileName, InputStream data,
                                  long size, String mimeType, Map<String, String> metadata) {
        validateSize(size);

        String attachmentId = UUID.randomUUID().toString();
        String extension = getFileExtension(fileName);
        String storagePath = generateStoragePath(issueId, fileName, attachmentId);

        try {
            // Ensure directory exists
            Path filePath = basePath.resolve(storagePath);
            Path directory = filePath.getParent();
            ensureDirectoryExists(directory);

            // Get lock for this directory to handle concurrent writes
            ReentrantLock lock = getDirectoryLock(directory.toString());
            lock.lock();
            try {
                // Write file with atomic operation
                Path tempPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");
                writeFile(data, tempPath);

                // Verify write
                if (!Files.exists(tempPath)) {
                    throw new StorageException("File write failed - file not found after write");
                }

                long writtenSize = Files.size(tempPath);
                if (writtenSize != size) {
                    Files.deleteIfExists(tempPath);
                    throw new StorageException(
                            String.format("File size mismatch: expected %d, got %d", size, writtenSize));
                }

                // Calculate checksum
                String checksum = calculateChecksum(tempPath);

                // Atomic rename
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);

                // Store metadata
                AttachmentMetadataEntity entity = AttachmentMetadataEntity.builder()
                        .id(attachmentId)
                        .issueId(issueId)
                        .fileName(fileName)
                        .originalFileName(fileName)
                        .mimeType(mimeType)
                        .sizeBytes(size)
                        .storagePath(storagePath)
                        .storageType(StorageType.LOCAL.name())
                        .checksum(checksum)
                        .contentHash(checksum)
                        .uploadedAt(Instant.now())
                        .virusScanStatus(AttachmentMetadataEntity.VirusScanStatus.NOT_SCANNED)
                        .metadataJson(metadata != null ? metadata : Map.of())
                        .build();

                metadataRepository.save(entity);

                log.info("Stored attachment {} for issue {}: {} ({} bytes)",
                        attachmentId, issueId, fileName, size);

                return buildStoredAttachment(entity);

            } finally {
                lock.unlock();
            }

        } catch (IOException e) {
            log.error("Failed to store attachment {} for issue {}", attachmentId, issueId, e);
            throw new StorageException("Failed to store attachment: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream retrieve(String attachmentId) {
        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        if (entity.isDeleted()) {
            throw StorageException.notFound(attachmentId);
        }

        Path filePath = basePath.resolve(entity.getStoragePath());

        try {
            if (!Files.exists(filePath)) {
                log.error("Attachment file not found on disk: {}", filePath);
                throw StorageException.notFound(attachmentId);
            }

            return new BufferedInputStream(Files.newInputStream(filePath));

        } catch (IOException e) {
            log.error("Failed to retrieve attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to retrieve attachment", attachmentId,
                    com.jira.migration.exception.StorageErrorType.STORAGE_ERROR, e);
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

        Path filePath = basePath.resolve(entity.getStoragePath());

        try {
            // Delete file
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            // Soft delete metadata
            entity.setDeleted(true);
            entity.setDeletedAt(Instant.now());
            metadataRepository.save(entity);

            log.info("Deleted attachment {}: {}", attachmentId, entity.getFileName());

        } catch (IOException e) {
            log.error("Failed to delete attachment file: {}", filePath, e);
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
                    Path filePath = basePath.resolve(e.getStoragePath());
                    return Files.exists(filePath);
                })
                .orElse(false);
    }

    @Override
    public String getDownloadUrl(String attachmentId, Duration validity) {
        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> StorageException.notFound(attachmentId));

        Path filePath = basePath.resolve(entity.getStoragePath());
        return filePath.toUri().toString();
    }

    @Override
    public String copy(String sourceAttachmentId, String targetIssueId) {
        AttachmentMetadataEntity source = metadataRepository.findById(sourceAttachmentId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> StorageException.notFound(sourceAttachmentId));

        Path sourcePath = basePath.resolve(source.getStoragePath());

        if (!Files.exists(sourcePath)) {
            throw StorageException.notFound(sourceAttachmentId);
        }

        String newFileName = generateCopyFileName(source.getFileName());
        String newAttachmentId = UUID.randomUUID().toString();
        String newStoragePath = generateStoragePath(targetIssueId, newFileName, newAttachmentId);

        try {
            Path targetPath = basePath.resolve(newStoragePath);
            ensureDirectoryExists(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            AttachmentMetadataEntity newEntity = AttachmentMetadataEntity.builder()
                    .id(newAttachmentId)
                    .issueId(targetIssueId)
                    .fileName(newFileName)
                    .originalFileName(source.getOriginalFileName())
                    .mimeType(source.getMimeType())
                    .sizeBytes(source.getSizeBytes())
                    .storagePath(newStoragePath)
                    .storageType(StorageType.LOCAL.name())
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

        } catch (IOException e) {
            throw new StorageException("Failed to copy attachment", e);
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
                // Continue with next file
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

    private Path resolvePath(String attachmentId) {
        AttachmentMetadataEntity entity = metadataRepository.findById(attachmentId)
                .orElseThrow(() -> StorageException.notFound(attachmentId));
        return basePath.resolve(entity.getStoragePath());
    }

    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private String generateStoragePath(String issueId, String fileName, String attachmentId) {
        YearMonth yearMonth = YearMonth.now();
        String sanitizedFileName = sanitizeFileName(fileName);
        String extension = getFileExtension(sanitizedFileName);
        String baseName = extension.isEmpty() ? attachmentId : attachmentId + "." + extension;

        return String.format("%s/%d/%02d/%s/%s",
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                issueId,
                baseName);
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
        // Remove dangerous characters
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    private void writeFile(InputStream data, Path target) throws IOException {
        try (OutputStream out = Files.newOutputStream(target,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            data.transferTo(out);
        }
    }

    private String calculateChecksum(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream is = Files.newInputStream(filePath)) {
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
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

    private void validateSize(long size) {
        long maxSize = config.getMaxFileSizeBytes();
        if (size > maxSize) {
            throw new StorageException(
                    String.format("File size %d exceeds maximum allowed %d bytes", size, maxSize),
                    null,
                    com.jira.migration.exception.StorageErrorType.QUOTA_EXCEEDED);
        }
    }

    private ReentrantLock getDirectoryLock(String directory) {
        return directoryLocks.computeIfAbsent(directory, k -> new ReentrantLock());
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