package com.avionics_systems.migration.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of a successful attachment upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredAttachment {

    private String attachmentId;
    private String issueId;
    private String fileName;
    private String storagePath;
    private String storageType;
    private long sizeBytes;
    private String mimeType;
    private String checksum;
    private String contentHash;
    private Instant uploadedAt;
    private String downloadUrl;
    private java.util.Map<String, String> metadata;
}