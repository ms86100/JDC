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
public class WorkflowStepModel {
    private String id;
    private String name;
    @Builder.Default
    private Map<String, String> meta = new LinkedHashMap<>();
    @Builder.Default
    private List<WorkflowActionModel> actions = new ArrayList<>();
}
