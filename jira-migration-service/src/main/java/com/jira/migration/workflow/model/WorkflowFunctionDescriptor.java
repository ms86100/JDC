package com.jira.migration.workflow.model;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowFunctionDescriptor {
    private String type;
    private String className;
    @Builder.Default
    private Map<String, String> args = new LinkedHashMap<>();
    private String conditionLogicType;
}
