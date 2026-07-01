package com.jira.migration.workflow.validation;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowXmlValidationReport {
    private boolean valid;
    private String workflowName;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<String> unsupportedFeatures = new ArrayList<>();
    @Builder.Default
    private List<String> executionRisks = new ArrayList<>();
    private int stepCount;
    private int transitionCount;
    private int globalTransitionCount;
    private boolean hasUnreachableSteps;
    private boolean hasDeadEnds;
    private Map<String, Object> graphJson;
    private Map<String, Object> compatibilityMatrix;
}
