package com.avionics_systems.version.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionBuildReferenceResponse {
    private UUID id;
    private UUID versionId;
    private String buildNumber;
    private String buildUrl;
    private String buildStatus;
    private String branchName;
    private String commitSha;
    private String commitMessage;
    private String authorName;
    private String authorEmail;
    private UUID triggeredBy;
    private LocalDateTime triggeredAt;
}