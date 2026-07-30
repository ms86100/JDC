package com.avionics_systems.migration.storage;

import com.avionics_systems.migration.config.storage.AttachmentStorageConfig;
import com.avionics_systems.migration.config.storage.StorageType;
import com.avionics_systems.migration.dto.attachment.AttachmentMetadata;
import com.avionics_systems.migration.dto.attachment.StoredAttachment;
import com.avionics_systems.migration.entity.attachment.AttachmentMetadataEntity;
import com.avionics_systems.migration.exception.StorageException;
import com.avionics_systems.migration.repository.attachment.AttachmentMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.Map.Entry;

/**
 * S3-compatible storage implementation for attachments.
 * Supports AWS S3, MinIO, DigitalOcean Spaces, and other S3-compatible services.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "attachment.storage.storage-type", havingValue = "S3")
public class S3StorageService implements AttachmentStorageService {

    private final AttachmentStorageConfig config;
    private final AttachmentMetadataRepository metadataRepository;
    private S3Client s3Client;
    private String bucket;

    @Autowired
    public S3StorageService(AttachmentStorageConfig config,
                           AttachmentMetadataRepository metadataRepository) {
        this.config = config;
        this.metadataRepository = metadataRepository;
        this.bucket = config.getS3().getBucket();
    }

    @PostConstruct
    public void init() {
        try {
            if (config.getS3() == null || config.getS3().getAccessKey() == null) {
                log.info("S3 storage not configured, skipping initialization");
                this.s3Client = null;
                return;
            }

            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    config.getS3().getAccessKey(),
                    config.getS3().getSecretKey()
            );

            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(config.getS3().isPathStyleAccess())
                    .build();

            var builder = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .serviceConfiguration(s3Config);

            if (config.getS3().getEndpoint() != null && !config.getS3().getEndpoint().isEmpty()) {
                builder.endpointOverride(URI.create(config.getS3().getEndpoint()));
            }

            builder.region(Region.of(config.getS3().getRegion()));

            this.s3Client = builder.build();

            // Ensure bucket exists
            ensureBucketExists();

            log.info("S3 storage initialized for bucket: {}", bucket);

        } catch (Exception e) {
            log.error("Failed to initialize S3 storage", e);
            throw new RuntimeException("Failed to initialize S3 storage", e);
        }
    }

    private void ensureBucketExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.headBucket(headRequest);
        } catch (NoSuchBucketException e) {
            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.createBucket(createRequest);
            log.info("Created S3 bucket: {}", bucket);
        }
    }

    @Override
    public StoredAttachment store(String issueId, String fileName, InputStream data,
                                  long size, String mimeType, Map<String, String> metadata) {
        validateSize(size);

        String attachmentId = UUID.randomUUID().toString();
        String s3Key = generateS3Key(issueId, fileName, attachmentId);

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

            // Upload to S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentLength(size)
                    .contentType(mimeType)
                    .metadata(convertMetadataToS3Format(metadata))
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));

            // Store metadata in database
            AttachmentMetadataEntity entity = AttachmentMetadataEntity.builder()
                    .id(attachmentId)
                    .issueId(issueId)
                    .fileName(fileName)
                    .originalFileName(fileName)
                    .mimeType(mimeType)
                    .sizeBytes(size)
                    .storagePath(s3Key)
                    .storageType(StorageType.S3.name())
                    .checksum(checksum)
                    .contentHash(checksum)
                    .uploadedAt(Instant.now())
                    .virusScanStatus(AttachmentMetadataEntity.VirusScanStatus.NOT_SCANNED)
                    .metadataJson(metadata != null ? metadata : Map.of())
                    .build();

            metadataRepository.save(entity);

            log.info("Stored attachment {} in S3 for issue {}: {} ({} bytes)",
                    attachmentId, issueId, fileName, size);

            return buildStoredAttachment(entity);

        } catch (S3Exception e) {
            log.error("S3 error storing attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to store attachment in S3: " + e.getMessage(),
                    attachmentId, com.avionics_systems.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.S3, e);
        } catch (IOException e) {
            log.error("IO error storing attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to process attachment data", attachmentId,
                    com.avionics_systems.migration.exception.StorageErrorType.STORAGE_ERROR, e);
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
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(entity.getStoragePath())
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);

            // Wrap in BufferedInputStream for better performance
            return new BufferedInputStream(response);

        } catch (NoSuchKeyException e) {
            log.error("S3 object not found for attachment {}: {}", attachmentId, e.getMessage());
            throw StorageException.notFound(attachmentId);
        } catch (S3Exception e) {
            log.error("S3 error retrieving attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to retrieve attachment from S3",
                    attachmentId, com.avionics_systems.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.S3, e);
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
            // Delete from S3
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(entity.getStoragePath())
                    .build();
            s3Client.deleteObject(deleteRequest);

            // Soft delete metadata
            entity.setDeleted(true);
            entity.setDeletedAt(Instant.now());
            metadataRepository.save(entity);

            log.info("Deleted attachment {} from S3: {}", attachmentId, entity.getFileName());

        } catch (S3Exception e) {
            log.error("S3 error deleting attachment {}: {}", attachmentId, e.getMessage(), e);
            throw new StorageException("Failed to delete attachment from S3",
                    attachmentId, com.avionics_systems.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.S3, e);
        }
    }

    @Override
    public boolean exists(String attachmentId) {
        return metadataRepository.findById(attachmentId)
                .filter(e -> !e.isDeleted())
                .map(e -> {
                    try {
                        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(e.getStoragePath())
                                .build();
                        s3Client.headObject(headRequest);
                        return true;
                    } catch (NoSuchKeyException ex) {
                        return false;
                    } catch (Exception ex) {
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
            // Generate a simple URL for now - presigning requires additional setup
            String baseUrl = s3Client.utilities().getUrl(b ->
                b.bucket(bucket).key(entity.getStoragePath()).build()).toString();

            // For signed URLs, in production you'd use the presigner API
            // This is a simplified version
            return baseUrl;

        } catch (Exception e) {
            log.error("Failed to generate download URL for {}: {}", attachmentId, e.getMessage());
            throw new StorageException("Failed to generate download URL",
                    attachmentId, com.avionics_systems.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.S3, e);
        }
    }

    @Override
    public String copy(String sourceAttachmentId, String targetIssueId) {
        AttachmentMetadataEntity source = metadataRepository.findById(sourceAttachmentId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> StorageException.notFound(sourceAttachmentId));

        String newFileName = generateCopyFileName(source.getFileName());
        String newAttachmentId = UUID.randomUUID().toString();
        String newS3Key = generateS3Key(targetIssueId, newFileName, newAttachmentId);

        try {
            // Copy object in S3
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(source.getStoragePath())
                    .destinationBucket(bucket)
                    .destinationKey(newS3Key)
                    .build();
            s3Client.copyObject(copyRequest);

            // Create new metadata entry
            AttachmentMetadataEntity newEntity = AttachmentMetadataEntity.builder()
                    .id(newAttachmentId)
                    .issueId(targetIssueId)
                    .fileName(newFileName)
                    .originalFileName(source.getOriginalFileName())
                    .mimeType(source.getMimeType())
                    .sizeBytes(source.getSizeBytes())
                    .storagePath(newS3Key)
                    .storageType(StorageType.S3.name())
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

        } catch (S3Exception e) {
            log.error("Failed to copy attachment {} to S3: {}", sourceAttachmentId, e.getMessage());
            throw new StorageException("Failed to copy attachment",
                    sourceAttachmentId, com.avionics_systems.migration.exception.StorageErrorType.STORAGE_ERROR,
                    StorageType.S3, e);
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

    private String generateS3Key(String issueId, String fileName, String attachmentId) {
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

    private PutObjectRequest buildPutObjectRequest(String bucket, String key, byte[] content,
                                                 String mimeType, Map<String, String> metadata) {
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimeType)
                .metadata(metadata)
                .build();
    }

    private Map<String, String> convertMetadataToS3Format(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (Entry<String, String> entry : metadata.entrySet()) {
            // S3 user metadata must be prefixed with x-amz-meta-
            result.put("x-amz-meta-" + entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void validateSize(long size) {
        long maxSize = config.getMaxFileSizeBytes();
        if (size > maxSize) {
            throw new StorageException(
                    String.format("File size %d exceeds maximum allowed %d bytes", size, maxSize),
                    null,
                    com.avionics_systems.migration.exception.StorageErrorType.QUOTA_EXCEEDED);
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