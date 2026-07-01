package com.jira.migration.dto;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgressResponse {
    private UUID jobId;
    private String jobStatus;
    private String errorMessage;
    private Double progressPercentage;
    private Integer totalEntities;
    private Integer processedEntities;
    private Integer failedEntities;
    private Integer completedEntities;
    private Integer pendingEntities;
    private Integer processingEntities;
    private Integer skippedEntities;
    private String currentPhase;
    private String currentEntityType;
    private Long elapsedTimeMs;
    private Long estimatedTimeRemainingMs;
    private List<EntityTypeProgress> entityProgress;
    private Map<String, StageProgress> stages;
    private List<Map<String, Object>> recentLogs;
    private Long attachmentBytesWritten;
    private Integer attachmentsCompleted;
    private Integer incrementalSkipped;
    private Integer attachmentChunkIndex;
    private Integer attachmentChunkTotal;
    private String attachmentCurrentFile;
    private Boolean attachmentChunked;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageProgress {
        private Integer completed;
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityTypeProgress {
        private String entityType;
        private Integer total;
        private Integer completed;
        private Integer failed;
        private Integer pending;
        private Integer processing;
    }
}