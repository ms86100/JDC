package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_instance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "current_state", length = 100)
    private String currentState;

    @Column(columnDefinition = "TEXT")
    private String stateHistoryJson;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Parse the state history JSON and return list of state transitions.
     * Each transition contains: fromState, toState, transitionedBy, transitionedAt, comment
     */
    public List<StateTransition> getStateHistory() {
        if (stateHistoryJson == null || stateHistoryJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(stateHistoryJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, StateTransition.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Inner class representing a state transition in the workflow history
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StateTransition {
        private String fromState;
        private String toState;
        private UUID transitionedBy;
        private LocalDateTime transitionedAt;
        private String comment;
    }
}