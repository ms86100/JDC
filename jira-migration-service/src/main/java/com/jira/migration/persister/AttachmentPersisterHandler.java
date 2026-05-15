package com.jira.migration.persister;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.exception.*;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.service.clients.*;
import com.jira.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Track created attachments for rollback
    private final List<String> createdAttachmentIds = new ArrayList<>();

    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024; // 10MB default
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
            Long maxSize = (Long) attachmentData.getOrDefault("maxSizeBytes", MAX_ATTACHMENT_SIZE);
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

            // Build upload request
            AttachmentServiceClient.AttachmentUploadRequest uploadRequest =
                    AttachmentServiceClient.AttachmentUploadRequest.builder()
                            .issueId(issueId)
                            .fileName(fileName)
                            .content(fileContent)
                            .size(fileSize)
                            .mimeType(mimeType)
                            .uploadedBy(uploadedBy)
                            .build();

            // Call real attachment service
            AttachmentResponse response = uploadAttachmentWithRetry(uploadRequest);
            String attachmentId = response.getId();

            // Track for potential rollback
            createdAttachmentIds.add(attachmentId);

            // Update entity status
            updateEntityStatus(jobId, issueKey + ":" + fileName, attachmentId, "ATTACHMENT", true);

            result.setSuccess(true);
            result.setAttachmentId(UUID.fromString(attachmentId));
            result.setFileName(fileName);
            result.setFileSize(fileSize);

            log.info("Persisted attachment {} for issue {} ({} bytes)", fileName, issueKey, fileSize);

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

        long maxSize = (Long) options.getOrDefault("maxSizeBytes", MAX_ATTACHMENT_SIZE);
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
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getAttachmentId() { return attachmentId; }
        public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
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