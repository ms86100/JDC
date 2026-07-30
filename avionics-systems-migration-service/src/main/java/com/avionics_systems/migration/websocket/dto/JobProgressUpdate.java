package com.avionics_systems.migration.websocket.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgressUpdate {
    private String jobId;
    private int progressPercentage;
    private int processedEntities;
    private int totalEntities;
    private int failedEntities;
    private String currentStage;
    private String currentEntityType;
    private String logMessage;
    private Instant timestamp;
    private List<EntityProgress> entityProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityProgress {
        private String entityType;
        private int total;
        private int completed;
        private int failed;
        private int pending;
        private int processing;
    }

    public static JobProgressUpdate fromProgressResponse(com.avionics_systems.migration.dto.JobProgressResponse response) {
        return JobProgressUpdate.builder()
                .jobId(response.getJobId().toString())
                .progressPercentage(response.getProgressPercentage() != null ? response.getProgressPercentage().intValue() : 0)
                .processedEntities(response.getProcessedEntities() != null ? response.getProcessedEntities() : 0)
                .totalEntities(response.getTotalEntities() != null ? response.getTotalEntities() : 0)
                .failedEntities(response.getFailedEntities() != null ? response.getFailedEntities() : 0)
                .currentStage(response.getCurrentPhase())
                .currentEntityType(response.getCurrentEntityType())
                .timestamp(Instant.now())
                .entityProgress(response.getEntityProgress() != null ?
                        response.getEntityProgress().stream()
                                .map(ep -> EntityProgress.builder()
                                        .entityType(ep.getEntityType())
                                        .total(ep.getTotal() != null ? ep.getTotal() : 0)
                                        .completed(ep.getCompleted() != null ? ep.getCompleted() : 0)
                                        .failed(ep.getFailed() != null ? ep.getFailed() : 0)
                                        .pending(ep.getPending() != null ? ep.getPending() : 0)
                                        .processing(ep.getProcessing() != null ? ep.getProcessing() : 0)
                                        .build())
                                .toList() : null)
                .build();
    }
}