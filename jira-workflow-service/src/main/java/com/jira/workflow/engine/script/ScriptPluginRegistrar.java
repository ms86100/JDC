package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import com.jira.workflow.engine.plugin.WorkflowPluginRegistry;
import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScriptPluginRegistrar implements ApplicationRunner {

    private final WorkflowPluginRegistry pluginRegistry;
    private final ScriptExecutionService scriptExecutionService;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptEngineProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Script engine is disabled, skipping plugin registration");
            return;
        }
        registerAllEnabledScripts();
    }

    public void registerAllEnabledScripts() {
        List<ScriptDefinition> scripts = scriptDefinitionRepository.findByIsEnabledTrueOrderByNameAsc();
        log.info("Registering {} enabled scripts as workflow plugin providers", scripts.size());
        for (ScriptDefinition script : scripts) {
            registerScript(script);
        }
    }

    public void registerScript(ScriptDefinition script) {
        String key = script.getScriptKey();

        switch (script.getScriptType()) {
            case ScriptDefinition.TYPE_CONDITION ->
                    pluginRegistry.registerCondition(key,
                            context -> scriptExecutionService.evaluateCondition(key, context));
            case ScriptDefinition.TYPE_VALIDATOR ->
                    pluginRegistry.registerValidator(key,
                            context -> scriptExecutionService.evaluateValidator(key, context));
            case ScriptDefinition.TYPE_POST_FUNCTION ->
                    pluginRegistry.registerPostFunction(key,
                            context -> scriptExecutionService.executePostFunction(key, context));
            default -> log.warn("Unknown script type '{}' for script '{}'", script.getScriptType(), key);
        }
        log.debug("Registered script {} as {} provider", key, script.getScriptType());
    }

    public void refreshScript(ScriptDefinition script) {
        unregisterAllTypes(script.getScriptKey());
        if (Boolean.TRUE.equals(script.getIsEnabled())) {
            registerScript(script);
        }
    }

    public void unregisterScript(ScriptDefinition script) {
        unregisterAllTypes(script.getScriptKey());
        log.info("Unregistered script: {} (type: {})", script.getScriptKey(), script.getScriptType());
    }

    private void unregisterAllTypes(String key) {
        pluginRegistry.unregisterCondition(key);
        pluginRegistry.unregisterValidator(key);
        pluginRegistry.unregisterPostFunction(key);
    }
}
