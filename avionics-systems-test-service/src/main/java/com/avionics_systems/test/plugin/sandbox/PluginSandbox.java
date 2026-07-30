package com.avionics_systems.test.plugin.sandbox;

import com.avionics_systems.test.plugin.hook.PluginHook;
import com.avionics_systems.test.plugin.hook.PluginHook.HookContext;
import com.avionics_systems.test.plugin.hook.PluginHook.HookResult;
import com.avionics_systems.test.plugin.hook.PluginHook.HookType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class PluginSandbox {

    private static final long MAX_EXECUTION_TIME_MS = 30_000;
    private static final long MAX_MEMORY_MB = 128;
    private static final int MAX_HOOKS_PER_PLUGIN = 10;

    private final Set<String> allowedApis;
    private final Map<String, AuditEntry> auditLog = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors() * 2, 20));

    public PluginSandbox() {
        this.allowedApis = new HashSet<>();
        initializeAllowedApis();
    }

    public PluginSandbox(Set<String> additionalAllowedApis) {
        this.allowedApis = new HashSet<>();
        initializeAllowedApis();
        this.allowedApis.addAll(additionalAllowedApis);
    }

    private void initializeAllowedApis() {
        allowedApis.add("java.lang.String");
        allowedApis.add("java.lang.Integer");
        allowedApis.add("java.lang.Long");
        allowedApis.add("java.lang.Boolean");
        allowedApis.add("java.lang.Double");
        allowedApis.add("java.lang.Float");
        allowedApis.add("java.util.List");
        allowedApis.add("java.util.Map");
        allowedApis.add("java.util.Set");
        allowedApis.add("java.util.HashMap");
        allowedApis.add("java.util.ArrayList");
        allowedApis.add("java.util.HashSet");
        allowedApis.add("java.time.LocalDateTime");
        allowedApis.add("java.time.Instant");
        allowedApis.add("com.avionics_systems.test.plugin.hook.PluginHook");
        allowedApis.add("com.avionics_systems.test.plugin.hook.PluginHook$HookContext");
        allowedApis.add("com.avionics_systems.test.plugin.hook.PluginHook$HookResult");
        allowedApis.add("com.avionics_systems.test.plugin.hook.PluginHook$HookType");
    }

    /**
     * Execute a plugin hook in the sandbox with timeout and resource limits.
     */
    public HookResult executeHook(String pluginId, PluginHook hook, HookType hookType, HookContext context) {
        Instant startTime = Instant.now();

        if (!isAllowedPlugin(pluginId)) {
            audit(pluginId, "EXECUTION_DENIED", "Plugin not authorized for execution");
            return HookResult.failure("Plugin not authorized for execution");
        }

        audit(pluginId, "EXECUTION_STARTED", "Hook type: " + hookType.getEventName());

        try {
            Future<HookResult> future = executor.submit(() -> {
                return invokeHook(hook, hookType, context);
            });

            HookResult result = future.get(MAX_EXECUTION_TIME_MS, TimeUnit.MILLISECONDS);

            Duration duration = Duration.between(startTime, Instant.now());
            audit(pluginId, "EXECUTION_COMPLETED",
                    String.format("Hook: %s, Duration: %dms, Success: %b",
                            hookType.getEventName(), duration.toMillis(), result.isSuccess()));

            return result;

        } catch (TimeoutException e) {
            Duration duration = Duration.between(startTime, Instant.now());
            audit(pluginId, "EXECUTION_TIMEOUT",
                    String.format("Exceeded %dms timeout after %dms", MAX_EXECUTION_TIME_MS, duration.toMillis()));
            return HookResult.failure("Plugin execution timed out after " + MAX_EXECUTION_TIME_MS + "ms");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            audit(pluginId, "EXECUTION_INTERRUPTED", e.getMessage());
            return HookResult.failure("Plugin execution was interrupted");

        } catch (ExecutionException e) {
            audit(pluginId, "EXECUTION_ERROR", e.getCause().getMessage());
            return HookResult.failure("Plugin execution failed: " + e.getCause().getMessage());

        } catch (Exception e) {
            audit(pluginId, "EXECUTION_ERROR", e.getMessage());
            return HookResult.failure("Unexpected error during plugin execution: " + e.getMessage());
        }
    }

    private HookResult invokeHook(PluginHook hook, HookType hookType, HookContext context) {
        switch (hookType) {
            case TEST_CREATED:
                return hook.onTestCreated(context);
            case TEST_EXECUTION_STARTED:
                return hook.onTestExecutionStarted(context);
            case TEST_EXECUTION_COMPLETED:
                return hook.onTestExecutionCompleted(context);
            case PRECONDITION_EVALUATED:
                return hook.onPreconditionEvaluated(context);
            case COVERAGE_CALCULATED:
                return hook.onCoverageCalculated(context);
            default:
                return HookResult.failure("Unknown hook type: " + hookType);
        }
    }

    /**
     * Check if a plugin is allowed to execute.
     */
    public boolean isAllowedPlugin(String pluginId) {
        return pluginId != null && !pluginId.isEmpty();
    }

    /**
     * Add a plugin to the allowlist.
     */
    public void allowPlugin(String pluginId) {
        audit(pluginId, "PLUGIN_ALLOWED", "Plugin added to allowlist");
    }

    /**
     * Remove a plugin from the allowlist.
     */
    public void disallowPlugin(String pluginId) {
        audit(pluginId, "PLUGIN_DISALLOWED", "Plugin removed from allowlist");
    }

    /**
     * Check if an API class is whitelisted.
     */
    public boolean isApiAllowed(String className) {
        return allowedApis.contains(className);
    }

    /**
     * Add an API to the whitelist.
     */
    public void allowApi(String className) {
        allowedApis.add(className);
    }

    /**
     * Remove an API from the whitelist.
     */
    public void disallowApi(String className) {
        allowedApis.remove(className);
    }

    /**
     * Get allowed APIs.
     */
    public Set<String> getAllowedApis() {
        return new HashSet<>(allowedApis);
    }

    /**
     * Record an audit entry for a plugin action.
     */
    private void audit(String pluginId, String action, String details) {
        AuditEntry entry = new AuditEntry(
                pluginId,
                action,
                details,
                Instant.now(),
                Thread.currentThread().getName()
        );
        auditLog.put(pluginId + ":" + System.currentTimeMillis(), entry);
    }

    /**
     * Get audit log for a specific plugin.
     */
    public List<AuditEntry> getAuditLog(String pluginId) {
        return auditLog.values().stream()
                .filter(e -> pluginId.equals(e.pluginId))
                .sorted(Comparator.comparing(e -> e.timestamp))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all audit entries.
     */
    public List<AuditEntry> getAllAuditEntries() {
        return new ArrayList<>(auditLog.values());
    }

    /**
     * Clear audit log.
     */
    public void clearAuditLog() {
        auditLog.clear();
    }

    /**
     * Get audit statistics.
     */
    public AuditStats getAuditStats() {
        long totalExecutions = auditLog.values().stream()
                .filter(e -> "EXECUTION_COMPLETED".equals(e.action))
                .count();
        long failedExecutions = auditLog.values().stream()
                .filter(e -> "EXECUTION_ERROR".equals(e.action) || "EXECUTION_TIMEOUT".equals(e.action))
                .count();
        long deniedExecutions = auditLog.values().stream()
                .filter(e -> "EXECUTION_DENIED".equals(e.action))
                .count();

        return new AuditStats(totalExecutions, failedExecutions, deniedExecutions, auditLog.size());
    }

    /**
     * Shutdown the sandbox executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class AuditEntry {
        public final String pluginId;
        public final String action;
        public final String details;
        public final Instant timestamp;
        public final String threadName;

        public AuditEntry(String pluginId, String action, String details, Instant timestamp, String threadName) {
            this.pluginId = pluginId;
            this.action = action;
            this.details = details;
            this.timestamp = timestamp;
            this.threadName = threadName;
        }
    }

    public static class AuditStats {
        public final long totalExecutions;
        public final long failedExecutions;
        public final long deniedExecutions;
        public final long totalEntries;

        public AuditStats(long totalExecutions, long failedExecutions, long deniedExecutions, long totalEntries) {
            this.totalExecutions = totalExecutions;
            this.failedExecutions = failedExecutions;
            this.deniedExecutions = deniedExecutions;
            this.totalEntries = totalEntries;
        }

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) (totalExecutions - failedExecutions) / totalExecutions : 0.0;
        }
    }

    public long getMaxExecutionTimeMs() {
        return MAX_EXECUTION_TIME_MS;
    }

    public long getMaxMemoryMb() {
        return MAX_MEMORY_MB;
    }
}
