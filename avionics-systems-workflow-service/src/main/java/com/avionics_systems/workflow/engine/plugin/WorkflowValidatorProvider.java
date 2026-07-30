package com.avionics_systems.workflow.engine.plugin;

import java.util.Map;
import java.util.Optional;

public interface WorkflowValidatorProvider {
    Optional<String> validate(Map<String, Object> context);
}
