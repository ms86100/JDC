package com.jira.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
