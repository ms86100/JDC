package com.jira.migration.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Metadata for a stored attachment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentMetadata {

    private String id;
    private String issueId;
    private String fileName;
    private String originalFileName;
    private String mimeType;
    private long sizeBytes;
    private String storagePath;
    private String storageType;
    private String checksum;
    private String uploadedBy;
    private Instant uploadedAt;
    private String contentHash;
    private VirusScanStatus virusScanStatus;
    private java.util.Map<String, String> metadata;

    public enum VirusScanStatus {
        PENDING,
        CLEAN,
        INFECTED,
        ERROR,
        NOT_SCANNED
    }
}