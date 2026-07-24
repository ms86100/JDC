package com.jira.attachment.controller;

import com.jira.attachment.dto.AttachmentResponse;
import com.jira.attachment.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Issue attachment management API")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload attachment", description = "Upload a file attachment to an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @Parameter(description = "Issue ID") @RequestParam UUID issueId,
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Uploader ID") @RequestParam(required = false) UUID uploaderId,
            @Parameter(description = "Uploader name") @RequestParam(required = false) String uploaderName) {

        AttachmentResponse response = attachmentService.uploadAttachment(issueId, file, uploaderId, uploaderName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issue/{issueId}")
    @Operation(summary = "Get attachments by issue", description = "Get all attachments for an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments retrieved successfully")
    })
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByIssue(issueId));
    }

    @GetMapping
    @Operation(summary = "List attachments", description = "Get all attachments or filter by issue")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(
            @Parameter(description = "Issue ID filter") @RequestParam(required = false) UUID issueId) {
        if (issueId != null) {
            return ResponseEntity.ok(attachmentService.getAttachmentsByIssue(issueId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{attachmentId}")
    @Operation(summary = "Get attachment", description = "Get attachment metadata by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment found"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<AttachmentResponse> getAttachment(
            @Parameter(description = "Attachment ID") @PathVariable UUID attachmentId) {
        return ResponseEntity.ok(attachmentService.getAttachment(attachmentId));
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download attachment", description = "Download the attachment file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File downloaded"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<Resource> downloadAttachment(
            @Parameter(description = "Attachment ID") @PathVariable UUID attachmentId) {

        Resource resource = attachmentService.downloadAttachment(attachmentId);
        AttachmentResponse metadata = attachmentService.getAttachment(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Delete attachment", description = "Delete an attachment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachment deleted"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<Void> deleteAttachment(
            @Parameter(description = "Attachment ID") @PathVariable UUID attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issue/{issueId}")
    @Operation(summary = "Delete all attachments for issue", description = "Delete all attachments for a specific issue")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachments deleted")
    })
    public ResponseEntity<Void> deleteAttachmentsByIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        attachmentService.deleteAttachmentsByIssue(issueId);
        return ResponseEntity.noContent().build();
    }
}