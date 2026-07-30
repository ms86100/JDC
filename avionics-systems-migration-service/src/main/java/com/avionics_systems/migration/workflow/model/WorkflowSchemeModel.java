package com.avionics_systems.migration.workflow.model;

import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSchemeModel {
    private String name;
    private String defaultWorkflow;
    @Builder.Default
    private Map<String, String> meta = new LinkedHashMap<>();
    @Builder.Default
    private List<WorkflowSchemeMapping> mappings = new ArrayList<>();
    @Builder.Default
    private List<WorkflowSchemeProjectAssociation> projectAssociations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSchemeMapping {
        private String issueType;
        private String workflow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSchemeProjectAssociation {
        private String projectKey;
        private String scheme;
        private boolean active;
    }
}
