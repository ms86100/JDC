package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a Workflow in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WorkflowDto {

    @EqualsAndHashCode.Include
    private String id;

    private String name;
    private String description;
    private String status;
    private boolean isDefault;
    private boolean isActive;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String createdBy;
    private String updatedBy;
    private List<WorkflowStep> steps;
    private List<WorkflowTransition> transitions;

    /**
     * Represents a step/status in the workflow.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStep {
        private String id;
        private String name;
        private int order;
        private String statusCategory;
        private String icon;
        private String color;
    }

    /**
     * Represents a transition between workflow steps.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowTransition {
        private String id;
        private String name;
        private String fromStep;
        private String toStep;
        private String trigger;
        private List<String> conditions;
        private List<String> postFunctions;
        private List<String> validators;
        private boolean isGlobal;
        private boolean isInitial;
    }
}