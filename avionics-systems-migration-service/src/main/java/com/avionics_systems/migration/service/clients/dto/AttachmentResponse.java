package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Attachment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AttachmentResponse {

    @EqualsAndHashCode.Include
    private String id;

    private String issueId;
    private String filename;
    private String mimeType;
    private Long size;
    private String uploaderId;
    private String uploaderEmail;
    private LocalDateTime created;
    private String fileUrl;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private String mediaType;
    private String checksum;
    private Integer downloadCount;
    private boolean success;
    private String errorMessage;
}