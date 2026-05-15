package com.jira.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new Workflow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {

    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;
    private boolean isDefault;
    private List<WorkflowStepRequest> steps;
    private List<WorkflowTransitionRequest> transitions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStepRequest {
        private String name;
        private int order;
        private String statusCategory;
        private String icon;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowTransitionRequest {
        private String name;
        private String fromStep;
        private String toStep;
        private String trigger;
        private List<String> conditions;
        private List<String> postFunctions;
        private List<String> validators;
    }
}