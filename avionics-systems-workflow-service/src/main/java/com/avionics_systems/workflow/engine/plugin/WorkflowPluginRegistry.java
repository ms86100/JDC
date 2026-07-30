package com.avionics_systems.workflow.engine.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
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

    public List<String> listValidatorKeys() {
        return validators.keySet().stream().sorted().toList();
    }

    public void executePostFunction(String key, Map<String, Object> context) {
        WorkflowPostFunctionProvider p = postFunctions.get(key);
        if (p != null) {
            p.execute(context);
        } else {
            log.warn("No post-function provider registered for key: {}", key);
        }
    }

    public Optional<String> validateWithProvider(String key, Map<String, Object> context) {
        WorkflowValidatorProvider p = validators.get(key);
        if (p != null) {
            return p.validate(context);
        }
        log.warn("No validator provider registered for key: {}", key);
        return Optional.of("Unknown script validator: " + key);
    }

    public List<String> listPostFunctionKeys() {
        return postFunctions.keySet().stream().sorted().toList();
    }

    public void unregisterCondition(String key) { conditions.remove(key); }
    public void unregisterValidator(String key) { validators.remove(key); }
    public void unregisterPostFunction(String key) { postFunctions.remove(key); }

    public boolean hasCondition(String key) { return conditions.containsKey(key); }
    public boolean hasValidator(String key) { return validators.containsKey(key); }
    public boolean hasPostFunction(String key) { return postFunctions.containsKey(key); }
}
