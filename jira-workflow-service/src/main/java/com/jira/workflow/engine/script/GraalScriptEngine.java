package com.jira.workflow.engine.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.config.ScriptEngineProperties;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

@Component
@Slf4j
public class GraalScriptEngine {

    private final ScriptEngineProperties properties;
    private final Engine graalEngine;
    private final ScheduledExecutorService timeoutScheduler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GraalScriptEngine(ScriptEngineProperties properties) {
        this.properties = properties;
        this.graalEngine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        this.timeoutScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "script-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        timeoutScheduler.shutdownNow();
        graalEngine.close();
    }

    public ScriptResult execute(String scriptBody, Map<String, Object> bindings, long timeoutMs) {
        if (!properties.isEnabled()) {
            return ScriptResult.error("Script engine is disabled", 0);
        }

        long start = System.currentTimeMillis();
        JdcConsole console = (JdcConsole) bindings.get("console");

        try (Context context = Context.newBuilder("js")
                .engine(graalEngine)
                .allowHostAccess(HostAccess.newBuilder(HostAccess.EXPLICIT)
                        .allowArrayAccess(true)
                        .allowListAccess(true)
                        .allowMapAccess(true)
                        .build())
                .allowHostClassLookup(className -> false)
                .allowIO(false)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowCreateProcess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .resourceLimits(ResourceLimits.newBuilder()
                        .statementLimit(properties.getMaxStatements(), null)
                        .build())
                .option("js.ecmascript-version", "2022")
                .option("js.strict", "true")
                .build()) {

            Value jsBindings = context.getBindings("js");
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                jsBindings.putMember(entry.getKey(), entry.getValue());
            }

            Value result = executeWithTimeout(context, scriptBody, timeoutMs);
            long elapsed = System.currentTimeMillis() - start;
            String consoleOutput = console != null ? console.getCapturedOutput() : null;
            return ScriptResult.success(convertResult(result), elapsed, consoleOutput);

        } catch (PolyglotException e) {
            long elapsed = System.currentTimeMillis() - start;
            String consoleOutput = console != null ? console.getCapturedOutput() : null;
            if (e.isCancelled() || e.isResourceExhausted()) {
                log.warn("Script exceeded resource limits after {}ms: {}", elapsed, e.getMessage());
                return ScriptResult.error("Script exceeded resource limits: " + e.getMessage(), elapsed, consoleOutput);
            }
            log.warn("Script execution error after {}ms: {}", elapsed, e.getMessage());
            return ScriptResult.error("Script error: " + e.getMessage(), elapsed, consoleOutput);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String consoleOutput = console != null ? console.getCapturedOutput() : null;
            log.error("Script execution failed after {}ms: {}", elapsed, e.getMessage());
            return ScriptResult.error("Execution failed: " + e.getMessage(), elapsed, consoleOutput);
        }
    }

    private Value executeWithTimeout(Context context, String scriptBody, long timeoutMs) {
        ScheduledFuture<?> cancelTask = timeoutScheduler.schedule(
                () -> context.close(true),
                timeoutMs, TimeUnit.MILLISECONDS);
        try {
            return context.eval("js", scriptBody);
        } finally {
            cancelTask.cancel(false);
        }
    }

    @SuppressWarnings("unchecked")
    private Object convertResult(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            return value.asDouble();
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(convertResult(value.getArrayElement(i)));
            }
            return list;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, convertResult(value.getMember(key)));
            }
            return map;
        }
        return value.toString();
    }
}
