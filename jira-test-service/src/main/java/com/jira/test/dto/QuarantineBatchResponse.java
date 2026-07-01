package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineBatchResponse {

    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<UUID> successfulIds;
    private List<BatchFailure> failures;
    private long processingTimeMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchFailure {
        private UUID testId;
        private String error;
        private String errorCode;
    }
}