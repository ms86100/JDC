package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceDto {

    private UUID id;
    private UUID definitionId;
    private String definitionName;
    private String entityType;
    private UUID entityId;
    private String currentState;
    private List<StateHistoryEntry> stateHistory;
    private UUID initiatedBy;
    private UUID assignedTo;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StateHistoryEntry {
        private String fromState;
        private String toState;
        private UUID transitionedBy;
        private LocalDateTime transitionedAt;
        private String comment;
    }
}