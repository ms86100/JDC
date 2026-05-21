package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepBulkResponse {

    private Integer totalRequested;
    private Integer successCount;
    private Integer failureCount;
    private Integer warningCount;

    // Detailed results
    private List<BulkOperationResult> results;

    // Summary by operation
    private Map<String, OperationSummary> operationSummaries;

    // Duration
    private Long durationMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkOperationResult {
        private UUID sharedStepId;
        private String sharedStepName;
        private Boolean success;
        private String message;
        private List<String> warnings;
        private List<String> affectedTests; // For operations that affect tests
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OperationSummary {
        private String operation;
        private Integer totalProcessed;
        private Integer succeeded;
        private Integer failed;
        private Integer warnings;
        private List<String> errors;
    }
}