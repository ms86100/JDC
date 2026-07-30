package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.util.List;

/**
 * Response DTO for batch issue creation operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchIssueResponse {

    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<IssueResponse> successful;
    private List<BatchFailure> failures;
    private long elapsedMs;

    /**
     * Represents a failed issue creation in a batch.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchFailure {
        private int index;
        private String originalIssueKey;
        private String errorCode;
        private String errorMessage;
    }
}