package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepSearchResponse {

    private List<SharedStepResponse> results;
    private Integer totalCount;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;

    // Search metadata
    private String searchQuery;
    private Double fuzzyMatchThreshold;
    private Long searchDurationMs;

    // Facets for filtering
    private SearchFacets facets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchFacets {
        private List<String> availableTags;
        private List<String> availableCategories;
        private UsageRangeFacet usageRange;
        private HealthStatusFacet healthStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageRangeFacet {
        private Long high; // 50+ usages
        private Long medium; // 10-49 usages
        private Long low; // 1-9 usages
        private Long unused; // 0 usages
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthStatusFacet {
        private Long healthy;
        private Long needsAttention;
        private Long highRisk;
    }
}