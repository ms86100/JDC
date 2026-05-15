package com.jira.attachment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadAttachmentRequest {

    @NotNull(message = "Issue ID is required")
    private UUID issueId;

    @NotBlank(message = "Filename is required")
    private String filename;

    private UUID uploaderId;
    private String uploaderName;
}