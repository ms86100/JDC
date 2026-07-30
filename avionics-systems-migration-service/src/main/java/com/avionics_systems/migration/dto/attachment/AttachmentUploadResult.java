package com.avionics_systems.migration.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of an attachment upload operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadResult {

    private String attachmentId;
    private String fileName;
    private long sizeBytes;
    private String mimeType;
    private String downloadUrl;
    private Instant uploadedAt;
    private boolean success;
    private String errorMessage;
}