package com.avionics_systems.workflow.engine.plugin;

import java.util.Map;

public interface WorkflowConditionProvider {
    boolean evaluate(Map<String, Object> context);
}
