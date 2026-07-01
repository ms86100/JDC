package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepSearchRequest {

    private UUID projectId;

    // Text Search
    private String query;
    private Boolean fuzzyMatch;
    private Double fuzzyThreshold; // 0.0 to 1.0

    // Filters
    private List<String> tags;
    private List<UUID> folderIds;
    private List<String> categories;

    // Version filters
    private Integer minVersion;
    private Integer maxVersion;
    private Boolean currentVersionOnly;

    // Usage filters
    private Integer minUsageCount;
    private Integer maxUsageCount;
    private String usageRange; // HIGH, MEDIUM, LOW, UNUSED

    // Status filters
    private Boolean includeArchived;
    private List<String> healthStatuses; // HEALTHY, NEEDS_ATTENTION, HIGH_RISK

    // Sorting
    private String sortBy; // name, usageCount, updatedAt, createdAt, relevance
    private String sortOrder; // ASC, DESC

    // Pagination
    private Integer page;
    private Integer pageSize;
}