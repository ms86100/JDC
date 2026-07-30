package com.avionics_systems.migration.persister;

import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.exception.*;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.service.ChunkedAttachmentUploadService;
import com.avionics_systems.migration.service.clients.*;
import com.avionics_systems.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Attachment Persister Handler
 * Handles attachment file processing using real attachment service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentPersisterHandler {

    private final EntityStatusRepository entityStatusRepository;
    private final AttachmentServiceClient attachmentServiceClient;
    private final IssueServiceClient issueServiceClient;
    private final ChunkedAttachmentUploadService chunkedAttachmentUploadService;

    // Track created attachments for rollback
    private final List<String> createdAttachmentIds = new ArrayList<>();

    private static final long DEFAULT_MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024; // 10MB — Legacy DC default

    @Value("${migration.attachment.max-size-bytes:10485760}")
    private long configuredMaxAttachmentSize;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip", "rar", "7z",
            "xml", "json", "html", "css", "js"
    );

    @Transactional(rollbackFor = Exception.class)
    public AttachmentPersistResult persistAttachment(
            Map<String, Object> attachmentData,
            byte[] fileContent,
            UUID jobId) {

        AttachmentPersistResult result = new AttachmentPersistResult();

        try {
            String issueKey = (String) attachmentData.get("issueKey");
            String issueId = (String) attachmentData.get("issueId");

            if (issueKey == null && issueId == null) {
                throw new IllegalArgumentException("Issue key or ID is required for attachment");
            }

            // Resolve issue ID if only key is provided
            if (issueId == null && issueKey != null) {
                issueId = resolveIssueId(issueKey, jobId);
                if (issueId == null) {
                    throw new EntityNotFoundException("Issue", issueKey);
                }
            }

            String fileName = (String) attachmentData.get("fileName");
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("File name is required");
            }

            // Validate file size
            long fileSize = fileContent != null ? fileContent.length : 0;
            long maxSize = attachmentData.get("maxSizeBytes") instanceof Number n
                    ? n.longValue()
                    : configuredMaxAttachmentSize;
            if (fileSize > maxSize) {
                throw new IllegalArgumentException(
                        "File size " + fileSize + " exceeds maximum allowed " + maxSize);
            }

            // Validate file extension
            String extension = getFileExtension(fileName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                log.warn("Attachment has non-standard extension: {}", extension);
            }

            String mimeType = (String) attachmentData.getOrDefault("mimeType", detectMimeType(fileName));
            String uploadedBy = (String) attachmentData.get("authorId");

            String expectedChecksum = (String) attachmentData.get("expectedChecksum");
            ChunkedAttachmentUploadService.UploadResult uploadResult =
                    chunkedAttachmentUploadService.upload(
                            jobId,
                            issueId,
                            fileName,
                            fileContent,
                            mimeType,
                            uploadedBy,
                            expectedChecksum);
            String attachmentId = uploadResult.attachmentId();
            String verifiedChecksum = uploadResult.checksum();

            // Track for potential rollback
            createdAttachmentIds.add(attachmentId);

            // Update entity status
            updateEntityStatus(jobId, issueKey + ":" + fileName, attachmentId, "ATTACHMENT", true);

            result.setSuccess(true);
            result.setAttachmentId(parseUuid(attachmentId));
            result.setFileName(fileName);
            result.setFileSize(fileSize);
            result.setChecksum(verifiedChecksum);
            result.setChunked(uploadResult.chunked());

            log.info("Persisted attachment {} for issue {} ({} bytes, chunked={}, checksum={})",
                    fileName, issueKey, fileSize, uploadResult.chunked(), verifiedChecksum);

        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (EntityNotFoundException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (ServiceClientException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Attachment service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to upload attachment: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist attachment: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist attachment: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Upload attachment with retry logic for transient failures.
     */
    private AttachmentResponse uploadAttachmentWithRetry(AttachmentServiceClient.AttachmentUploadRequest request) {
        int maxRetries = 3;
        long baseDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return attachmentServiceClient.uploadAttachment(request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Attachment upload failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, maxRetries, baseDelayMs * attempt, e.getMessage());
                    try {
                        Thread.sleep(baseDelayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new MigrationException("Attachment upload failed after " + maxRetries + " attempts");
    }

    private String resolveIssueId(String issueKey, UUID jobId) {
        try {
            Optional<IssueResponse> issue = issueServiceClient.getIssueByKey(issueKey);
            return issue.map(IssueResponse::getId).orElse(null);
        } catch (Exception e) {
            log.debug("Could not resolve issue ID for key {}: {}", issueKey, e.getMessage());
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String detectMimeType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "xml" -> "application/xml";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    /**
     * Validate attachment before processing.
     */
    public ValidationResult validateAttachment(
            String fileName,
            long fileSize,
            String contentType,
            Map<String, Object> options) {

        List<String> errors = new ArrayList<>();

        if (fileName == null || fileName.isBlank()) {
            errors.add("File name is required");
        }
        if (fileName != null && fileName.length() > 255) {
            errors.add("File name exceeds maximum length of 255 characters");
        }

        long maxSize = (Long) options.getOrDefault("maxSizeBytes", configuredMaxAttachmentSize);
        if (fileSize > maxSize) {
            errors.add("File size " + fileSize + " exceeds maximum " + maxSize);
        }

        String ext = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            errors.add("File extension ." + ext + " may not be allowed");
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(createValidationErrors(errors, "ATTACHMENT_VALIDATION"))
                .warnings(List.of())
                .build();
    }

    private List<ValidationResult.ValidationError> createValidationErrors(List<String> messages, String errorCode) {
        List<ValidationResult.ValidationError> result = new ArrayList<>();
        for (String msg : messages) {
            ValidationResult.ValidationError err = new ValidationResult.ValidationError();
            err.setErrorCode(errorCode);
            err.setMessage(msg);
            result.add(err);
        }
        return result;
    }

    private void updateEntityStatus(UUID jobId, String sourceKey, String targetId,
                                    String type, boolean success) {
        try {
            EntityStatus status = entityStatusRepository
                    .findByJobIdAndEntityTypeAndSourceIdentifier(jobId, type, sourceKey)
                    .orElse(EntityStatus.builder()
                            .jobId(jobId)
                            .entityType(type)
                            .sourceIdentifier(sourceKey)
                            .build());

            status.setTargetId(targetId);
            status.setStatus(success ? "SUCCESS" : "FAILED");
            status.setProcessedAt(java.time.LocalDateTime.now());

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    /**
     * Rollback created attachments on failure.
     */
    public void rollbackCreatedAttachments() {
        log.info("Rolling back {} created attachments", createdAttachmentIds.size());
        for (String attachmentId : createdAttachmentIds) {
            try {
                attachmentServiceClient.deleteAttachment(attachmentId);
                log.debug("Rolled back attachment: {}", attachmentId);
            } catch (Exception e) {
                log.error("Failed to rollback attachment {}: {}", attachmentId, e.getMessage());
            }
        }
        createdAttachmentIds.clear();
    }

    /**
     * Clear rollback tracking.
     */
    public void clearRollbackTracking() {
        createdAttachmentIds.clear();
    }

    public static class AttachmentPersistResult {
        private boolean success;
        private UUID attachmentId;
        private String fileName;
        private long fileSize;
        private String checksum;
        private boolean chunked;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getAttachmentId() { return attachmentId; }
        public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }
        public boolean isChunked() { return chunked; }
        public void setChunked(boolean chunked) { this.chunked = chunked; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    @lombok.Data
    @lombok.Builder
    public static class ValidationResult {
        private boolean valid;
        private List<ValidationError> errors;
        private List<ValidationWarning> warnings;

        @lombok.Data
        public static class ValidationError {
            private String errorCode;
            private String message;
        }

        @lombok.Data
        public static class ValidationWarning {
            private String warningCode;
            private String message;
        }
    }
}