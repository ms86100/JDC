package com.jira.migration.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * Result of an attachment download operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDownloadResult {

    private String attachmentId;
    private String fileName;
    private String mimeType;
    private long sizeBytes;
    private InputStream contentStream;
    private String checksum;
}