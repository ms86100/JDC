package com.jira.version.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionDeploymentResponse {
    private UUID id;
    private UUID versionId;
    private String deploymentId;
    private String environment;
    private String deploymentUrl;
    private String buildNumber;
    private String buildUrl;
    private String commitSha;
    private UUID deployedBy;
    private LocalDateTime deployedAt;
    private String status;
    private Map<String, Object> metadata;
}