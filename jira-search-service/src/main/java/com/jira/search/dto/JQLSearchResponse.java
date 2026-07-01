package com.jira.search.dto;

import com.jira.search.entity.JQLQuery;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JQLSearchResponse {
    @Builder.Default
    private List<IssueSummary> issues = new ArrayList<>();
    private long totalCount;
    private int page;
    private int pageSize;
    private String message;
    private JQLQuery parsedQuery;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueSummary {
        private String issueKey;
        private String summary;
        private String status;
        private String issueType;
        private String assignee;
        private String reporter;
        private String priority;
    }
}