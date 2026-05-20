package com.jira.workflow.engine.plugin;

import java.util.Map;
import java.util.Optional;

public interface WorkflowValidatorProvider {
    Optional<String> validate(Map<String, Object> context);
}
