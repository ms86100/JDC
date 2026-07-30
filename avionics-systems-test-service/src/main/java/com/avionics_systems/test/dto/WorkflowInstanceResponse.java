package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceResponse {
    private UUID id;
    private UUID definitionId;
    private String entityType;
    private UUID entityId;
    private String currentState;
    private String stateHistoryJson;
    private UUID initiatedBy;
    private UUID assignedTo;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}