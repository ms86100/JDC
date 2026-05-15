package com.jira.migration.websocket.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCompleteNotification {
    private String jobId;
    private String status; // COMPLETED, FAILED, PARTIAL_SUCCESS
    private int successCount;
    private int failedCount;
    private Instant completedAt;
    private String downloadUrl;
    private ImportSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportSummary {
        private int totalProcessed;
        private int totalFailed;
        private long durationMs;
        private java.util.Map<String, Integer> processedByType;
        private java.util.List<String> warnings;
        private java.util.List<String> errors;
    }
}