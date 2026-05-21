package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepDetailResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;

    // Steps
    private List<SharedStepDto> steps;
    private Integer stepCount;

    // Version Info
    private Integer currentVersion;
    private Integer totalVersions;

    // Tags & Categories
    private List<String> tags;
    private List<String> categories;
    private List<String> labels;

    // Usage
    private Integer usageCount;
    private Integer activeTestsCount;

    // Templates
    private Boolean isTemplate;
    private UUID templateId; // If this was created from a template

    // Metadata
    private UUID folderId;
    private String folderName;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private String healthStatus;
    private Double usageScore;
    private Integer popularityRank;

    // Related shared steps
    private List<RelatedSharedStep> relatedSteps;

    // Version history summary
    private VersionSummary lastModifiedVersion;
    private List<VersionSummary> recentVersions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedSharedStep {
        private UUID id;
        private String name;
        private Integer usageCount;
        private String relationshipType; // SIMILAR, DEPENDENT, PARENT, CHILD
        private Double similarityScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VersionSummary {
        private Integer versionNumber;
        private String changeSummary;
        private LocalDateTime createdAt;
        private UUID createdBy;
        private Boolean isCurrent;
    }
}