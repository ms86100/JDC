package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowDto {
    private String id;
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private Boolean isSystem;
    private List<WorkflowTransitionDto> transitions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WorkflowTransitionDto {
        private String id;
        private String name;
        private String fromStatus;
        private String toStatus;
        private Integer sequence;
        private List<String> validators;
        private List<String> postFunctions;
    }
}