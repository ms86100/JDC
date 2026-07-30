package com.avionics_systems.document.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionResponse {

    private UUID id;
    private UUID documentId;
    private Integer versionNumber;
    private String content;
    private String changeSummary;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private String contentHash;
}