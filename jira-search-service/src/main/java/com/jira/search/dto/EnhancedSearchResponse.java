package com.jira.search.dto;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced search response with facets, filters, and suggestions.
 * F6-US001/US006: Search Enhancement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedSearchResponse {

    private List<EnhancedSearchResult> results;
    private long totalCount;
    private int page;
    private int size;
    private int totalPages;
    private String query;
    private String originalQuery;
    private Long searchTimeMs;
    private List<SearchFacet> facets;
    private List<String> suggestions;
    private Map<String, List<String>> filters;
    private SortInfo sort;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnhancedSearchResult {
        private UUID id;
        private String entityType;
        private UUID entityId;
        private String title;
        private String content;
        private float relevance;
        private Map<String, Object> highlights;
        private List<String> matchedFields;
        private String projectKey;
        private String projectName;
        private String status;
        private String issueType;
        private UUID assigneeId;
        private String assigneeName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFacet {
        private String field;
        private String label;
        private FacetType type;
        private List<FacetBucket> buckets;
        private long missingCount;
        private long otherCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetBucket {
        private String value;
        private String label;
        private long count;
        private boolean selected;
    }

    public enum FacetType {
        TERMS, RANGE, DATE_HISTOGRAM
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortInfo {
        private String field;
        private SortOrder order;
        private List<SortField> secondarySort;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortField {
        private String field;
        private SortOrder order;
    }

    public enum SortOrder {
        ASC, DESC
    }
}