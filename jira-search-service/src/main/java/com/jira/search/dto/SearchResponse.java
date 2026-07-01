package com.jira.search.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private List<SearchResult> results;
    private long totalCount;
    private int page;
    private int size;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private UUID id;
        private String entityType;
        private UUID entityId;
        private String title;
        private String content;
        private float relevance;
    }
}