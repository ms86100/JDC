package com.avionics_systems.migration.service;

import com.avionics_systems.migration.service.clients.AttachmentServiceClient;
import com.avionics_systems.migration.service.clients.dto.AttachmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Chunked attachment upload with SHA-256 checksum verification (spec parity).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkedAttachmentUploadService {

    private final AttachmentServiceClient attachmentServiceClient;
    private final AttachmentUploadProgressReporter progressReporter;

    @Value("${migration.attachment.chunk-size-bytes:2097152}")
    private int chunkSizeBytes;

    @Value("${migration.attachment.chunked-threshold-bytes:2097152}")
    private long chunkedThresholdBytes;

    public UploadResult upload(String issueId, String fileName, byte[] content, String mimeType,
                               String uploadedBy, String expectedChecksum) {
        return upload(null, issueId, fileName, content, mimeType, uploadedBy, expectedChecksum);
    }

    public UploadResult upload(UUID jobId, String issueId, String fileName, byte[] content, String mimeType,
                               String uploadedBy, String expectedChecksum) {
        String checksum = sha256Hex(content);
        if (expectedChecksum != null && !expectedChecksum.isBlank()
                && !checksum.equalsIgnoreCase(expectedChecksum.trim())) {
            throw new IllegalArgumentException(
                    "Attachment checksum mismatch: expected " + expectedChecksum + ", got " + checksum);
        }

        AttachmentServiceClient.AttachmentUploadRequest request =
                AttachmentServiceClient.AttachmentUploadRequest.builder()
                        .issueId(issueId)
                        .fileName(fileName)
                        .content(content)
                        .size(content.length)
                        .mimeType(mimeType)
                        .uploadedBy(uploadedBy)
                        .build();

        if (content.length <= chunkedThresholdBytes) {
            AttachmentResponse response = attachmentServiceClient.uploadAttachment(request);
            return new UploadResult(response.getId(), checksum, false, content.length);
        }

        String sessionId = attachmentServiceClient.initChunkedUpload(
                issueId, fileName, content.length, checksum, mimeType, uploadedBy);
        // Stash metadata for local-session fallback assembly
        attachmentServiceClient.prepareLocalChunkSession(sessionId, issueId, fileName, mimeType, uploadedBy);
        int totalChunks = (int) Math.ceil((double) content.length / chunkSizeBytes);
        int chunkIndex = 0;
        long bytesSoFar = 0;
        for (int offset = 0; offset < content.length; offset += chunkSizeBytes) {
            int len = Math.min(chunkSizeBytes, content.length - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(content, offset, chunk, 0, len);
            attachmentServiceClient.uploadChunk(sessionId, chunkIndex, chunk);
            bytesSoFar += len;
            progressReporter.reportChunkProgress(jobId, fileName, chunkIndex + 1, totalChunks, bytesSoFar);
            chunkIndex++;
        }
        AttachmentResponse completed = attachmentServiceClient.completeChunkedUpload(sessionId, checksum);
        log.info("Chunked upload complete: {} chunks, checksum={}", chunkIndex, checksum);
        return new UploadResult(completed.getId(), checksum, true, content.length);
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record UploadResult(String attachmentId, String checksum, boolean chunked, long sizeBytes) {
    }
}
