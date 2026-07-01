package com.jira.migration.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an attachment export operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentExportResult {

    private String attachmentId;
    private String fileName;
    private byte[] content;
    private String mimeType;
    private long sizeBytes;
    private String checksum;
}