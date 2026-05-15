package com.jira.search.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JQLSearchRequest {
    private String jql;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int pageSize = 50;
    @Builder.Default
    private String[] fields = null;  // Specific fields to return, null for all
    @Builder.Default
    private boolean expandChangelog = false;
}