package com.jira.migration.workflow.model;

import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowActionModel {
    private String id;
    private String name;
    private String sourceStepId;
    private boolean global;
    private boolean initial;
    private String view;
    @Builder.Default
    private Map<String, String> meta = new LinkedHashMap<>();
    @Builder.Default
    private List<WorkflowFunctionDescriptor> validators = new ArrayList<>();
    @Builder.Default
    private List<WorkflowFunctionDescriptor> conditions = new ArrayList<>();
    @Builder.Default
    private List<WorkflowFunctionDescriptor> postFunctions = new ArrayList<>();
    @Builder.Default
    private List<WorkflowResultModel> results = new ArrayList<>();
}
