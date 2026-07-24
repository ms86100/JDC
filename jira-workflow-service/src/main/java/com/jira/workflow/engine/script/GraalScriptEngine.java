package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.Source;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

@Component
@Slf4j
public class GraalScriptEngine {

    private static final int MAX_RESULT_DEPTH = 20;

    private final ScriptEngineProperties properties;
    private final Engine graalEngine;
    private final ScheduledExecutorService timeoutScheduler;
    private final ConcurrentHashMap<String, Source> sourceCache = new ConcurrentHashMap<>();

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
        return execute(null, scriptBody, bindings, timeoutMs);
    }

    public ScriptResult execute(String scriptKey, String scriptBody, Map<String, Object> bindings, long timeoutMs) {
        if (!properties.isEnabled()) {
            return ScriptResult.error("Script engine is disabled", 0);
        }

        long start = System.currentTimeMillis();
        JdcConsole console = bindings.get("console") instanceof JdcConsole c ? c : null;

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
                .allowPolyglotAccess(PolyglotAccess.NONE)
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

            Source source = getOrCompileSource(scriptKey, scriptBody);
            Value result = executeWithTimeout(context, source, timeoutMs);
            long elapsed = System.currentTimeMillis() - start;
            String consoleOutput = console != null ? console.getCapturedOutput() : null;
            return ScriptResult.success(convertResult(result, 0), elapsed, consoleOutput);

        } catch (PolyglotException e) {
            long elapsed = System.currentTimeMillis() - start;
            String consoleOutput = console != null ? console.getCapturedOutput() : null;
            if (e.isCancelled() || e.isResourceExhausted()) {
                log.warn("Script exceeded resource limits after {}ms: {}", elapsed, e.getMessage());
                return ScriptResult.error("Script exceeded resource limits: " + e.getMessage(), elapsed, consoleOutput);
            }
            log.warn("Script execution error after {}ms: {}", elapsed, e.getMessage());
            return ScriptResult.error("Script error: " + e.getMessage(), elapsed, consoleOutput);
        } catch (Exception | StackOverflowError e) {
            long elapsed = System.currentTimeMillis() - start;
            String consoleOutput = console != null ? console.getCapturedOutput() : null;
            log.error("Script execution failed after {}ms: {}", elapsed, e.getMessage());
            return ScriptResult.error("Execution failed: " + e.getMessage(), elapsed, consoleOutput);
        }
    }

    private Source getOrCompileSource(String scriptKey, String scriptBody) {
        if (scriptKey == null) {
            return Source.create("js", scriptBody);
        }
        return sourceCache.compute(scriptKey, (k, existing) -> {
            if (existing != null && existing.getCharacters().toString().equals(scriptBody)) {
                return existing;
            }
            return Source.newBuilder("js", scriptBody, scriptKey).buildLiteral();
        });
    }

    public void invalidateCache(String scriptKey) {
        sourceCache.remove(scriptKey);
    }

    private Value executeWithTimeout(Context context, Source source, long timeoutMs) {
        ScheduledFuture<?> cancelTask = timeoutScheduler.schedule(() -> {
            try { context.close(true); } catch (Exception ignored) {}
        }, timeoutMs, TimeUnit.MILLISECONDS);
        try {
            return context.eval(source);
        } finally {
            cancelTask.cancel(false);
        }
    }

    public void parseOnly(String scriptBody) {
        try (Context context = Context.newBuilder("js")
                .engine(graalEngine)
                .allowHostAccess(HostAccess.EXPLICIT)
                .allowHostClassLookup(className -> false)
                .allowIO(false)
                .build()) {
            Source source = Source.newBuilder("js", scriptBody, "validation").buildLiteral();
            context.parse(source);
        }
    }

    private Object convertResult(Value value, int depth) {
        if (depth > MAX_RESULT_DEPTH) return "[max depth exceeded]";
        if (value == null || value.isNull()) return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            return value.asDouble();
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < Math.min(value.getArraySize(), 1000); i++) {
                list.add(convertResult(value.getArrayElement(i), depth + 1));
            }
            return list;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            int count = 0;
            for (String key : value.getMemberKeys()) {
                if (count++ >= 200) break;
                map.put(key, convertResult(value.getMember(key), depth + 1));
            }
            return map;
        }
        return value.toString();
    }
}
