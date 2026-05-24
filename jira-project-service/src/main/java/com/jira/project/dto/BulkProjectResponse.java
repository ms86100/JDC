package com.jira.project.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkProjectResponse {

    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<BulkOperationResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkOperationResult {
        private String projectId;
        private boolean success;
        private String message;
    }
}