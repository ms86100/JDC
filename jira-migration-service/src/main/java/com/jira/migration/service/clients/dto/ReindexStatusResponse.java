package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for reindex status operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexStatusResponse {

    private String entityType;
    private String status;
    private long totalDocuments;
    private long indexedDocuments;
    private long failedDocuments;
    private double progressPercentage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String currentPhase;
    private String errorMessage;
    private boolean success;
}