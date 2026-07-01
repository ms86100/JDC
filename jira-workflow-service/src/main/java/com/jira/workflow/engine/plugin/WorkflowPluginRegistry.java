package com.jira.workflow.engine.plugin;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extension point for custom conditions, validators, and post-functions (Jira app modules).
 */
@Component
public class WorkflowPluginRegistry {

    private final Map<String, WorkflowConditionProvider> conditions = new ConcurrentHashMap<>();
    private final Map<String, WorkflowValidatorProvider> validators = new ConcurrentHashMap<>();
    private final Map<String, WorkflowPostFunctionProvider> postFunctions = new ConcurrentHashMap<>();

    public void registerCondition(String key, WorkflowConditionProvider provider) {
        conditions.put(key, provider);
    }

    public void registerValidator(String key, WorkflowValidatorProvider provider) {
        validators.put(key, provider);
    }

    public void registerPostFunction(String key, WorkflowPostFunctionProvider provider) {
        postFunctions.put(key, provider);
    }

    public List<String> listConditionKeys() {
        return conditions.keySet().stream().sorted().toList();
    }

    public boolean evaluateCondition(String key, Map<String, Object> context) {
        WorkflowConditionProvider p = conditions.get(key);
        return p != null && p.evaluate(context);
    }

    /**
     * Gets a validator provider by key.
     */
    public WorkflowValidatorProvider getValidatorProvider(String key) {
        return validators.get(key);
    }

    /**
     * Lists all registered validator keys.
     */
    public List<String> listValidatorKeys() {
        return validators.keySet().stream().sorted().toList();
    }
}
