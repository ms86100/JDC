package com.jira.migration.mapper.attachment;

import com.jira.migration.dto.attachment.FileValidationResult;
import com.jira.migration.storage.AttachmentUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * Mapper for converting attachment data from various sources (CSV, Jira DC XML)
 * to internal AttachmentUploadRequest format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentDataMapper {

    /**
     * Map from CSV attachment row to AttachmentUploadRequest.
     *
     * Expected CSV columns:
     * - issueKey: Target issue key
     * - fileName: Original file name
     * - filePath: Path to file content (or base64 encoded content)
     * - mimeType: Content MIME type
     * - uploadedBy: User who uploaded
     * - uploadedAt: Upload timestamp
     */
    public AttachmentUploadRequest mapFromCsvRow(Map<String, String> row, String issueId) {
        String fileName = row.get("fileName");
        String mimeType = row.getOrDefault("mimeType", detectMimeType(fileName));
        String uploadedBy = row.get("uploadedBy");
        String uploadedAt = row.get("uploadedAt");

        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        if (uploadedBy != null) {
            metadata.put("originalAuthor", uploadedBy);
        }
        if (uploadedAt != null) {
            metadata.put("originalDate", uploadedAt);
        }
        metadata.put("source", "csv");

        return AttachmentUploadRequest.builder()
                .issueId(issueId)
                .fileName(fileName)
                .mimeType(mimeType)
                .uploadedBy(uploadedBy)
                .uploadedAt(uploadedAt != null ? parseInstant(uploadedAt) : Instant.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Map from Jira DC XML attachment element to AttachmentUploadRequest.
     *
     * @param xmlAttachment The XML attachment element data
     * @param issueId       Target issue ID
     * @return AttachmentUploadRequest ready for storage
     */
    public AttachmentUploadRequest mapFromJiraDcXml(Map<String, Object> xmlAttachment, String issueId) {
        String fileName = (String) xmlAttachment.get("fileName");
        String mimeType = (String) xmlAttachment.getOrDefault("mimeType", detectMimeType(fileName));
        Long size = parseSize(xmlAttachment.get("size"));
        String author = (String) xmlAttachment.get("author");
        String created = (String) xmlAttachment.get("created");

        // Map custom metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "jira_dc");
        metadata.put("originalId", (String) xmlAttachment.get("id"));
        metadata.put("attachmentNum", String.valueOf(xmlAttachment.get("attachmentNum")));

        // Copy any additional attributes
        for (Map.Entry<String, Object> entry : xmlAttachment.entrySet()) {
            if (entry.getValue() != null && !metadata.containsKey(entry.getKey())) {
                metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        return AttachmentUploadRequest.builder()
                .issueId(issueId)
                .fileName(fileName)
                .mimeType(mimeType)
                .uploadedBy(author)
                .uploadedAt(created != null ? parseInstant(created) : Instant.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Map from Base64-encoded file content to InputStream.
     *
     * @param base64Content Base64 encoded file content
     * @return InputStream of decoded content
     */
    public InputStream mapFromBase64(String base64Content) {
        byte[] decoded = Base64.getDecoder().decode(base64Content);
        return new ByteArrayInputStream(decoded);
    }

    /**
     * Validate mapped attachment data.
     *
     * @param request The attachment upload request to validate
     * @return Validation result
     */
    public FileValidationResult validateMapping(AttachmentUploadRequest request) {
        FileValidationResult result = FileValidationResult.success();
        List<String> errors = new ArrayList<>();

        if (request.getIssueId() == null || request.getIssueId().isBlank()) {
            errors.add("Issue ID is required");
        }

        if (request.getFileName() == null || request.getFileName().isBlank()) {
            errors.add("File name is required");
        }

        if (request.getFileName() != null && request.getFileName().length() > 255) {
            errors.add("File name exceeds maximum length of 255 characters");
        }

        if (request.getMimeType() == null || request.getMimeType().isBlank()) {
            errors.add("MIME type is required");
        }

        if (request.getSize() < 0) {
            errors.add("Invalid file size");
        }

        if (!errors.isEmpty()) {
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    /**
     * Detect MIME type from file name extension.
     */
    private String detectMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "7z" -> "application/x-7z-compressed";
            case "rar" -> "application/x-rar-compressed";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "doc" -> "application/msword";
            case "docx" ->
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" ->
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" ->
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }

    /**
     * Parse timestamp string to Instant.
     */
    private Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }

        try {
            // Try ISO format first
            return Instant.parse(timestamp);
        } catch (Exception e) {
            try {
                // Try epoch millis
                return Instant.ofEpochMilli(Long.parseLong(timestamp));
            } catch (Exception ex) {
                log.warn("Failed to parse timestamp '{}', using current time", timestamp);
                return Instant.now();
            }
        }
    }

    /**
     * Parse size value to long.
     */
    private long parseSize(Object size) {
        if (size == null) {
            return 0;
        }
        if (size instanceof Number) {
            return ((Number) size).longValue();
        }
        try {
            return Long.parseLong(size.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}