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
public class VersionComparisonResponse {
    private VersionSummaryDto version1;
    private VersionSummaryDto version2;
    private List<FieldDiff> fieldDiffs;
    private String changeMagnitude;
    private Double similarityScore;
    private List<AffectedTestDto> affectedTests;
    private LineageDto lineage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionSummaryDto {
        private UUID id;
        private String version;
        private Integer versionNumber;
        private RequirementVersion.RequirementVersionStatus status;
        private RequirementVersion.ChangeMagnitude changeMagnitude;
        private String changelog;
        private LocalDateTime createdAt;
        private LocalDateTime publishedAt;
        private UUID publishedBy;
        private String titleSnapshot;
        private String descriptionSnapshot;
        private UUID previousVersionId;
        private String previousVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDiff {
        private String field;
        private String type;
        private String v1;
        private String v2;
        private Integer v1Length;
        private Integer v2Length;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffectedTestDto {
        private UUID testId;
        private String testKey;
        private String linkType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineageDto {
        private String v1Version;
        private String v2Version;
        private LocalDateTime v1CreatedAt;
        private LocalDateTime v2CreatedAt;
        private Integer versionDistance;
        private Boolean isConsecutive;
    }
}