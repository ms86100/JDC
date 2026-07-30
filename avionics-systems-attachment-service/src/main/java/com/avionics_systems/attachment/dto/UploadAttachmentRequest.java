package com.avionics_systems.attachment.dto;

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

    @NotNull(message = "{validation.issueId.required}")
    private UUID issueId;

    @NotBlank(message = "{validation.filename.required}")
    private String filename;

    private UUID uploaderId;
    private String uploaderName;
}
