package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Workflow operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WorkflowResponse {

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
    private List<WorkflowStepResponse> steps;
    private List<WorkflowTransitionResponse> transitions;
    private boolean success;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStepResponse {
        private String id;
        private String name;
        private int order;
        private String statusCategory;
        private String icon;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowTransitionResponse {
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