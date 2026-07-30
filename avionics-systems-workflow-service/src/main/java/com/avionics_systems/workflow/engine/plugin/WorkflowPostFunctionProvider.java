package com.avionics_systems.workflow.engine.plugin;

import java.util.Map;

public interface WorkflowPostFunctionProvider {
    void execute(Map<String, Object> context);
}
