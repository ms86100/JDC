package com.avionics_systems.version.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionReleaseNoteResponse {
    private UUID id;
    private UUID versionId;
    private String content;
    private LocalDateTime generatedAt;
    private UUID generatedBy;
    private String contentHash;
}