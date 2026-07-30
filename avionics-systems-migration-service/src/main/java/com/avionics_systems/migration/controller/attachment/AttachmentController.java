package com.avionics_systems.migration.controller.attachment;

import com.avionics_systems.migration.dto.attachment.*;
import com.avionics_systems.migration.service.attachment.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST controller for attachment operations.
 */
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Upload attachment to an issue.
     */
    @PostMapping("/issue/{issueId}")
    public ResponseEntity<AttachmentUploadResult> uploadAttachment(
            @PathVariable String issueId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {

        log.info("Upload request for issue {}: {}", issueId, file.getOriginalFilename());

        try {
            AttachmentUploadResult result = attachmentService.uploadAttachment(
                    issueId, file, uploadedBy);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IOException e) {
            log.error("Failed to upload attachment for issue {}: {}", issueId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(AttachmentUploadResult.builder()
                            .fileName(file.getOriginalFilename())
                            .success(false)
                            .errorMessage("Failed to read file: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Download an attachment.
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String attachmentId) {
        log.info("Download request for attachment: {}", attachmentId);

        try {
            AttachmentDownloadResult result = attachmentService.downloadAttachment(attachmentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(result.getMimeType()));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(result.getFileName())
                    .build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(result.getSizeBytes())
                    .body(new InputStreamResource(result.getContentStream()));

        } catch (Exception e) {
            log.error("Failed to download attachment {}: {}", attachmentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get attachment metadata.
     */
    @GetMapping("/{attachmentId}")
    public ResponseEntity<AttachmentMetadata> getAttachmentMetadata(
            @PathVariable String attachmentId) {

        AttachmentMetadata metadata = attachmentService.downloadAttachment(attachmentId)
                .getAttachmentId() != null ?
                AttachmentMetadata.builder()
                        .id(attachmentId)
                        .build() : null;

        // Get actual metadata
        var attachments = attachmentService.getIssueAttachments(attachmentId);
        if (!attachments.isEmpty()) {
            return ResponseEntity.ok(attachments.get(0));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Get all attachments for an issue.
     */
    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<AttachmentMetadata>> getIssueAttachments(
            @PathVariable String issueId) {

        List<AttachmentMetadata> attachments = attachmentService.getIssueAttachments(issueId);
        return ResponseEntity.ok(attachments);
    }

    /**
     * Delete an attachment.
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable String attachmentId) {
        log.info("Delete request for attachment: {}", attachmentId);

        try {
            attachmentService.deleteAttachment(attachmentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete attachment {}: {}", attachmentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Copy attachments to another issue.
     */
    @PostMapping("/copy")
    public ResponseEntity<List<String>> copyAttachments(
            @RequestBody CopyAttachmentsRequest request) {

        log.info("Copy request: {} attachments to issue {}",
                request.getAttachmentIds().size(), request.getTargetIssueId());

        try {
            List<String> newIds = attachmentService.copyAttachmentsToIssue(
                    request.getAttachmentIds(),
                    request.getTargetIssueId()
            );
            return ResponseEntity.ok(newIds);
        } catch (Exception e) {
            log.error("Failed to copy attachments: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate a file before upload.
     */
    @PostMapping("/validate")
    public ResponseEntity<FileValidationResult> validateFile(
            @RequestParam("file") MultipartFile file) {

        try {
            FileValidationResult result = attachmentService.validateAttachment(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Failed to validate file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FileValidationResult.error("Failed to read file: " + e.getMessage()));
        }
    }

    /**
     * Get attachment count for an issue.
     */
    @GetMapping("/issue/{issueId}/count")
    public ResponseEntity<Map<String, Object>> getIssueAttachmentStats(
            @PathVariable String issueId) {

        int count = attachmentService.getAttachmentCount(issueId);
        long totalSize = attachmentService.getTotalAttachmentSize(issueId);

        return ResponseEntity.ok(Map.of(
                "issueId", issueId,
                "count", count,
                "totalSizeBytes", totalSize,
                "totalSizeMb", totalSize / (1024.0 * 1024.0)
        ));
    }

    /**
     * Get presigned download URL.
     */
    @GetMapping("/{attachmentId}/url")
    public ResponseEntity<Map<String, Object>> getDownloadUrl(
            @PathVariable String attachmentId,
            @RequestParam(value = "validityMinutes", defaultValue = "60") long validityMinutes) {

        try {
            String url = attachmentService.getDownloadUrl(
                    attachmentId, Duration.ofMinutes(validityMinutes));

            return ResponseEntity.ok(Map.of(
                    "attachmentId", attachmentId,
                    "downloadUrl", url,
                    "validityMinutes", validityMinutes
            ));
        } catch (Exception e) {
            log.error("Failed to generate download URL for {}: {}", attachmentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Request for copying attachments.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CopyAttachmentsRequest {
        private List<String> attachmentIds;
        private String targetIssueId;
    }
}