package com.jira.workflow.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportWorkflowDescriptorRequest {

    private UUID projectId;
    private String name;
    private String description;
    private boolean makeDefault;
    @Builder.Default
    private List<StepImport> steps = List.of();
    @Builder.Default
    private List<TransitionImport> transitions = List.of();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepImport {
        private String stepId;
        private String stepName;
        private UUID platformStatusId;
        private int sequence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionImport {
        private String sourceActionId;
        private String name;
        private UUID fromStatusId;
        private UUID toStatusId;
        private String screenId;
        private boolean global;
        private List<ComponentImport> validators;
        private List<ComponentImport> conditions;
        private List<ComponentImport> postFunctions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentImport {
        private String type;
        private String fieldName;
        private String value;
        private String configJson;
        private int sequence;
    }
}
