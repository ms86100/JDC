package com.avionics_systems.test.dto;

import com.avionics_systems.test.entity.RequirementVersion;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionStatisticsResponse {
    private UUID requirementId;
    private Long totalVersions;
    private Long draftVersions;
    private Long publishedVersions;
    private Long archivedVersions;
    private String latestVersion;
    private LocalDateTime firstCreatedAt;
    private Long versionsWithChangelog;
    private List<VersionSummary> recentVersions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionSummary {
        private UUID id;
        private String version;
        private Integer versionNumber;
        private RequirementVersion.RequirementVersionStatus status;
        private RequirementVersion.ChangeMagnitude changeMagnitude;
        private String changelog;
        private LocalDateTime createdAt;
    }
}