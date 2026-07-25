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
        java.util.Set<String> resolvedKeys = new java.util.HashSet<>();
        String resolvedBody = resolveIncludes(scriptBody, resolvedKeys);
        Map<String, Object> enrichedCtx = new java.util.HashMap<>(ctx);
        enrichedCtx.put("_resolvedIncludes", resolvedKeys);
        Map<String, Object> bindings = jdcScriptBindings.buildBindings(enrichedCtx);
        ScriptResult result = graalScriptEngine.execute(resolvedBody, bindings, properties.getConsoleTimeoutMs());

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
        java.util.Set<String> resolvedKeys = new java.util.HashSet<>();
        String resolvedBody = resolveIncludes(script.getScriptBody(), resolvedKeys);
        Map<String, Object> enrichedCtx = new java.util.HashMap<>(ctx);
        enrichedCtx.put("_resolvedIncludes", resolvedKeys);
        Map<String, Object> bindings = jdcScriptBindings.buildBindings(enrichedCtx);

        ScriptTracer tracer = new ScriptTracer(true);
        bindings.put("_tracer", tracer);

        // Wire tracer and mutation buffer into JdcApi
        JdcApi jdcApi = null;
        Object jdcObj = bindings.get("jdc");
        if (jdcObj instanceof JdcApi j) {
            jdcApi = j;
            jdcApi.setTracer(tracer);
        }
        MutationBuffer buffer = new MutationBuffer();
        if (jdcApi != null) {
            jdcApi.setMutationBuffer(buffer);
        }

        ScriptResult result = graalScriptEngine.execute(script.getScriptKey(), resolvedBody, bindings, properties.getTimeoutMs());

        // Auto-flush buffered mutations on success
        if (result.success() && jdcApi != null) {
            try { jdcApi.flush(); } catch (Exception e) { log.warn("Auto-flush failed: {}", e.getMessage()); }
        }

        logExecution(script.getId(), script.getScriptKey(), script.getScriptType(), executionMode, ctx, result, tracer);
        return result;
    }

    private String resolveIncludes(String scriptBody, java.util.Set<String> resolvedKeys) {
        if (scriptBody == null || !scriptBody.contains("include(")) return scriptBody;

        StringBuilder resolved = new StringBuilder();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:include\\.include|include)\\([\"']([a-z][a-z0-9-]{2,63})[\"']\\)");
        java.util.regex.Matcher matcher = pattern.matcher(scriptBody);

        while (matcher.find()) {
            String key = matcher.group(1);
            if (resolvedKeys.contains(key)) continue;
            resolvedKeys.add(key);
            scriptDefinitionRepository.findByScriptKey(key).ifPresent(lib -> {
                if (Boolean.TRUE.equals(lib.getIsEnabled())) {
                    resolved.append("// --- included: ").append(key).append(" ---\n");
                    resolved.append(lib.getScriptBody()).append("\n");
                }
            });
        }

        if (resolved.isEmpty()) return scriptBody;
        resolved.append("// --- main script ---\n");
        resolved.append(scriptBody);
        return resolved.toString();
    }

    private void logExecution(UUID scriptId, String scriptKey, String scriptType,
                              String executionMode, Map<String, Object> ctx, ScriptResult result) {
        logExecution(scriptId, scriptKey, scriptType, executionMode, ctx, result, null);
    }

    private void logExecution(UUID scriptId, String scriptKey, String scriptType,
                              String executionMode, Map<String, Object> ctx, ScriptResult result,
                              ScriptTracer tracer) {
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
                    .apiCallCount(tracer != null ? tracer.getApiCallCount() : 0)
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
