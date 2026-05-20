package com.jira.workflow.engine.plugin;

import java.util.Map;

public interface WorkflowPostFunctionProvider {
    void execute(Map<String, Object> context);
}
