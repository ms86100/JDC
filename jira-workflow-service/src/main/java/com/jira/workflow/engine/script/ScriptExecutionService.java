package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.entity.ScriptExecutionLog;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import com.jira.workflow.repository.ScriptExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptExecutionService {

    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptExecutionLogRepository executionLogRepository;
    private final GraalScriptEngine graalScriptEngine;
    private final JdcScriptBindings jdcScriptBindings;
    private final ScriptEngineProperties properties;

    public ScriptResult executeByKey(String scriptKey, Map<String, Object> workflowContext, String executionMode) {
        ScriptDefinition script = scriptDefinitionRepository.findByScriptKey(scriptKey)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "scriptKey", scriptKey));

        if (!Boolean.TRUE.equals(script.getIsEnabled())) {
            return ScriptResult.error("Script is disabled: " + scriptKey, 0);
        }

        return executeScript(script, workflowContext, executionMode);
    }

    public ScriptResult executeConsole(String scriptBody, String scriptType, Map<String, Object> mockContext) {
        Map<String, Object> ctx = mockContext != null ? mockContext : Map.of();
        Map<String, Object> bindings = jdcScriptBindings.buildBindings(ctx);
        ScriptResult result = graalScriptEngine.execute(scriptBody, bindings, properties.getConsoleTimeoutMs());

        logExecution(null, "console-test", scriptType, "CONSOLE", ctx, result);
        return result;
    }

    public boolean evaluateCondition(String scriptKey, Map<String, Object> ctx) {
        ScriptResult result = executeByKey(scriptKey, ctx, "WORKFLOW");
        if (!result.success()) {
            log.warn("Script condition '{}' failed: {}", scriptKey, result.errorMessage());
            return false;
        }
        return Boolean.TRUE.equals(result.value()) || "true".equalsIgnoreCase(String.valueOf(result.value()));
    }

    public Optional<String> evaluateValidator(String scriptKey, Map<String, Object> ctx) {
        ScriptResult result = executeByKey(scriptKey, ctx, "WORKFLOW");
        if (!result.success()) {
            return Optional.of("Script validator error: " + result.errorMessage());
        }
        Object val = result.value();
        if (val == null || (val instanceof String s && s.isBlank())) {
            return Optional.empty();
        }
        return Optional.of(val.toString());
    }

    public void executePostFunction(String scriptKey, Map<String, Object> ctx) {
        ScriptResult result = executeByKey(scriptKey, ctx, "WORKFLOW");
        if (!result.success()) {
            log.error("Script post-function '{}' failed: {}", scriptKey, result.errorMessage());
        }
    }

    private ScriptResult executeScript(ScriptDefinition script, Map<String, Object> ctx, String executionMode) {
        Map<String, Object> bindings = jdcScriptBindings.buildBindings(ctx);
        ScriptResult result = graalScriptEngine.execute(script.getScriptKey(), script.getScriptBody(), bindings, properties.getTimeoutMs());

        logExecution(script.getId(), script.getScriptKey(), script.getScriptType(), executionMode, ctx, result);
        return result;
    }

    private void logExecution(UUID scriptId, String scriptKey, String scriptType,
                              String executionMode, Map<String, Object> ctx, ScriptResult result) {
        try {
            ScriptExecutionLog logEntry = ScriptExecutionLog.builder()
                    .scriptId(scriptId)
                    .scriptKey(scriptKey)
                    .scriptType(scriptType != null ? scriptType : "UNKNOWN")
                    .executionMode(executionMode)
                    .issueId(parseUuid(ctx.get("issueId")))
                    .projectId(parseUuid(ctx.get("projectId")))
                    .userId(parseUuid(ctx.get("userId")))
                    .transitionId(parseUuid(ctx.get("transitionId")))
                    .success(result.success())
                    .resultValue(result.value() != null ? result.value().toString() : null)
                    .errorMessage(result.errorMessage())
                    .executionMs(result.executionMs())
                    .build();
            executionLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save script execution log: {}", e.getMessage());
        }
    }

    private UUID parseUuid(Object val) {
        if (val == null) return null;
        try {
            return UUID.fromString(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
