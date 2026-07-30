package com.avionics_systems.migration.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * Request object for uploading an attachment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadRequest {

    private String issueId;
    private String fileName;
    private InputStream data;
    private long size;
    private String mimeType;
    private String uploadedBy;
    private java.time.Instant uploadedAt;
    private java.util.Map<String, String> metadata;
}