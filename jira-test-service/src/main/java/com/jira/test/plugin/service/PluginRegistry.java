package com.jira.test.plugin.service;

import com.jira.test.plugin.entity.PluginManifest;
import com.jira.test.plugin.entity.PluginManifest.PluginStatus;
import com.jira.test.plugin.hook.PluginHook;
import com.jira.test.plugin.hook.PluginHook.HookContext;
import com.jira.test.plugin.hook.PluginHook.HookResult;
import com.jira.test.plugin.hook.PluginHook.HookType;
import com.jira.test.plugin.sandbox.PluginSandbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced service for managing plugin registration, lifecycle, dependency resolution, and hook invocation.
 * Provides centralized plugin management with thread-safe operations, versioning support, and safe hook execution.
 */
@Service
@Slf4j
public class PluginRegistry {

    private final Map<String, PluginManifest> manifests = new ConcurrentHashMap<>();
    private final Map<String, PluginHook> hooks = new ConcurrentHashMap<>();
    private final Map<String, Set<HookType>> pluginHooks = new ConcurrentHashMap<>();
    private final Map<HookType, List<String>> hookSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> pluginDependencies = new ConcurrentHashMap<>();
    private final Map<String, PluginVersion> pluginVersions = new ConcurrentHashMap<>();
    private final Map<String, List<HookResult>> hookHistory = new ConcurrentHashMap<>();
    private final PluginSandbox sandbox;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();
    private volatile boolean shuttingDown = false;

    public PluginRegistry() {
        this.sandbox = new PluginSandbox();
        for (HookType type : HookType.values()) {
            hookSubscriptions.put(type, new ArrayList<>());
        }
    }

    @PostConstruct
    public void initialize() {
        log.info("PluginRegistry initialized - automatic hook registration enabled");
        startHookHistoryCleanup();
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        log.info("Shutting down PluginRegistry - disabling all plugins");

        for (String pluginId : new ArrayList<>(manifests.keySet())) {
            try {
                disablePlugin(pluginId);
            } catch (Exception e) {
                log.error("Error disabling plugin {} during shutdown", pluginId, e);
            }
        }

        scheduler.shutdown();
        asyncExecutor.shutdown();
        sandbox.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Register a new plugin with its manifest and hooks.
     * Handles dependency resolution and version checking.
     */
    public PluginManifest registerPlugin(String pluginId, PluginManifest manifest, PluginHook hook) {
        return registerPlugin(pluginId, manifest, hook, Collections.emptySet());
    }

    /**
     * Register a new plugin with its manifest, hooks, and explicit dependencies.
     */
    public PluginManifest registerPlugin(String pluginId, PluginManifest manifest, PluginHook hook,
                                         Set<String> dependencies) {
        if (manifests.containsKey(pluginId)) {
            PluginManifest existing = manifests.get(pluginId);
            if (isVersionCompatible(existing.getVersion(), manifest.getVersion())) {
                throw new IllegalStateException("Plugin already registered with compatible version: " + pluginId);
            }
            log.info("Upgrading plugin {} from {} to {}", pluginId, existing.getVersion(), manifest.getVersion());
            upgradePlugin(pluginId, manifest, hook, dependencies);
            return manifest;
        }

        if (!dependencies.isEmpty()) {
            resolveDependencies(pluginId, dependencies);
        }

        manifest.setPluginId(pluginId);
        manifest.setInstalledAt(LocalDateTime.now());
        manifest.setStatus(PluginStatus.INSTALLED);

        manifests.put(pluginId, manifest);

        if (dependencies != null && !dependencies.isEmpty()) {
            pluginDependencies.put(pluginId, new HashSet<>(dependencies));
        }

        if (hook != null) {
            hooks.put(pluginId, hook);
            registerHookTypes(pluginId, hook.getHookTypes());

            try {
                hook.initialize(extractConfig(manifest));
                sandbox.allowPlugin(pluginId);
            } catch (Exception e) {
                manifest.markError();
                hooks.remove(pluginId);
                pluginHooks.remove(pluginId);
                sandbox.disallowPlugin(pluginId);
                throw new RuntimeException("Failed to initialize plugin: " + pluginId, e);
            }
        }

        storeVersion(pluginId, manifest.getVersion());
        log.info("Plugin registered successfully: {} v{}", pluginId, manifest.getVersion());
        return manifest;
    }

    /**
     * Upgrade an existing plugin to a new version.
     */
    private void upgradePlugin(String pluginId, PluginManifest newManifest, PluginHook newHook,
                               Set<String> dependencies) {
        PluginManifest oldManifest = manifests.get(pluginId);
        PluginHook oldHook = hooks.get(pluginId);

        if (oldHook != null) {
            try {
                oldHook.destroy();
            } catch (Exception e) {
                log.warn("Error destroying old plugin instance for {}: {}", pluginId, e.getMessage());
            }
        }

        newManifest.setId(oldManifest.getId());
        newManifest.setInstalledAt(oldManifest.getInstalledAt());
        newManifest.setEnabled(oldManifest.getEnabled());
        newManifest.setStatus(oldManifest.getEnabled() ? PluginStatus.ENABLED : PluginStatus.INSTALLED);

        manifests.put(pluginId, newManifest);

        if (newHook != null) {
            hooks.put(pluginId, newHook);
            registerHookTypes(pluginId, newHook.getHookTypes());

            try {
                newHook.initialize(extractConfig(newManifest));
            } catch (Exception e) {
                newManifest.markError();
                hooks.remove(pluginId);
                throw new RuntimeException("Failed to re-initialize plugin after upgrade: " + pluginId, e);
            }
        }

        if (dependencies != null) {
            pluginDependencies.put(pluginId, new HashSet<>(dependencies));
        }

        storeVersion(pluginId, newManifest.getVersion());
        log.info("Plugin upgraded: {} from {} to {}", pluginId, oldManifest.getVersion(), newManifest.getVersion());
    }

    /**
     * Resolve plugin dependencies - checks if all dependencies are satisfied.
     */
    private void resolveDependencies(String pluginId, Set<String> dependencies) {
        Set<String> unresolved = new HashSet<>();

        for (String dep : dependencies) {
            if (!manifests.containsKey(dep)) {
                unresolved.add(dep);
            } else {
                PluginManifest depManifest = manifests.get(dep);
                if (!depManifest.getEnabled()) {
                    log.warn("Dependency {} for plugin {} is not enabled", dep, pluginId);
                }
            }
        }

        if (!unresolved.isEmpty()) {
            throw new IllegalStateException("Unresolved dependencies for plugin " + pluginId + ": " + unresolved);
        }
    }

    /**
     * Check if two versions are compatible (same major version).
     */
    private boolean isVersionCompatible(String existing, String incoming) {
        try {
            String[] existingParts = existing.split("\\.");
            String[] incomingParts = incoming.split("\\.");

            int existingMajor = Integer.parseInt(existingParts[0]);
            int incomingMajor = Integer.parseInt(incomingParts[0]);

            return existingMajor == incomingMajor;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Store version history for a plugin.
     */
    private void storeVersion(String pluginId, String version) {
        PluginVersion pv = new PluginVersion(pluginId, version, LocalDateTime.now());
        pluginVersions.put(pluginId, pv);
    }

    /**
     * Unregister a plugin and clean up all its hooks.
     */
    public void unregisterPlugin(String pluginId) {
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        Set<String> dependents = getDependentPlugins(pluginId);
        if (!dependents.isEmpty()) {
            throw new IllegalStateException("Cannot unregister plugin with active dependents: " + dependents);
        }

        PluginHook hook = hooks.get(pluginId);
        if (hook != null) {
            try {
                hook.destroy();
            } catch (Exception e) {
                log.warn("Error during plugin {} cleanup: {}", pluginId, e.getMessage());
            }
            hooks.remove(pluginId);
        }

        Set<HookType> hookTypes = pluginHooks.remove(pluginId);
        if (hookTypes != null) {
            for (HookType type : hookTypes) {
                List<String> subscribers = hookSubscriptions.get(type);
                if (subscribers != null) {
                    subscribers.remove(pluginId);
                }
            }
        }

        pluginDependencies.remove(pluginId);
        pluginVersions.remove(pluginId);
        hookHistory.remove(pluginId);
        sandbox.disallowPlugin(pluginId);
        manifests.remove(pluginId);

        log.info("Plugin unregistered: {}", pluginId);
    }

    /**
     * Get all plugins that depend on a given plugin.
     */
    public Set<String> getDependentPlugins(String pluginId) {
        return pluginDependencies.entrySet().stream()
                .filter(e -> e.getValue().contains(pluginId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Enable a registered plugin with safe hook invocation.
     */
    public PluginManifest enablePlugin(String pluginId) {
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        if (manifest.getStatus() == PluginStatus.ERROR) {
            throw new IllegalStateException("Cannot enable plugin in error state: " + pluginId);
        }

        Set<String> deps = pluginDependencies.get(pluginId);
        if (deps != null) {
            for (String dep : deps) {
                PluginManifest depManifest = manifests.get(dep);
                if (depManifest == null) {
                    throw new IllegalStateException("Required dependency not found: " + dep);
                }
                if (!depManifest.getEnabled()) {
                    throw new IllegalStateException("Required dependency not enabled: " + dep);
                }
            }
        }

        manifest.enable();
        log.info("Plugin enabled: {}", pluginId);
        return manifest;
    }

    /**
     * Disable an enabled plugin.
     */
    public PluginManifest disablePlugin(String pluginId) {
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        Set<String> dependents = getDependentPlugins(pluginId);
        if (!dependents.isEmpty() && !shuttingDown) {
            throw new IllegalStateException("Cannot disable plugin with active dependents: " + dependents);
        }

        manifest.disable();
        log.info("Plugin disabled: {}", pluginId);
        return manifest;
    }

    /**
     * Get all plugins for a specific project.
     */
    public List<PluginManifest> getPlugins(String projectId) {
        return manifests.values().stream()
                .filter(m -> projectId.equals(m.getProjectId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all enabled plugins for a specific project.
     */
    public List<PluginManifest> getEnabledPlugins(String projectId) {
        return manifests.values().stream()
                .filter(m -> projectId.equals(m.getProjectId()))
                .filter(PluginManifest::getEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Get all plugins.
     */
    public List<PluginManifest> getAllPlugins() {
        return new ArrayList<>(manifests.values());
    }

    /**
     * Get a specific plugin by ID.
     */
    public PluginManifest getPlugin(String pluginId) {
        return manifests.get(pluginId);
    }

    /**
     * Get hook types implemented by a plugin.
     */
    public Set<HookType> getPluginHooks(String pluginId) {
        Set<HookType> hookTypes = pluginHooks.get(pluginId);
        return hookTypes != null ? new HashSet<>(hookTypes) : Collections.emptySet();
    }

    /**
     * Invoke a hook for all enabled plugins that subscribe to it (safe execution).
     */
    public List<HookResult> invokeHook(HookType hookType, String projectId, Map<String, Object> payload) {
        return invokeHook(hookType, projectId, payload, false);
    }

    /**
     * Invoke a hook with error isolation for all enabled plugins.
     */
    public List<HookResult> invokeHook(HookType hookType, String projectId, Map<String, Object> payload,
                                       boolean continueOnError) {
        List<String> subscribers = hookSubscriptions.get(hookType);
        if (subscribers == null || subscribers.isEmpty()) {
            return Collections.emptyList();
        }

        List<HookResult> results = new CopyOnWriteArrayList<>();

        for (String pluginId : subscribers) {
            PluginManifest manifest = manifests.get(pluginId);
            if (manifest == null || !manifest.getEnabled()) {
                continue;
            }

            if (!projectId.equals(manifest.getProjectId())) {
                continue;
            }

            PluginHook hook = hooks.get(pluginId);
            if (hook == null) {
                continue;
            }

            HookContext context = new HookContext(hookType, projectId, pluginId, payload);

            try {
                HookResult result = executeHookSafely(pluginId, hook, hookType, context);
                results.add(result);
                recordHookExecution(pluginId, result);
            } catch (Exception e) {
                HookResult failureResult = HookResult.failure("Hook execution failed: " + e.getMessage());
                results.add(failureResult);
                recordHookExecution(pluginId, failureResult);
                if (!continueOnError) {
                    log.error("Hook execution stopped due to error in plugin {}: {}", pluginId, e.getMessage());
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Execute a hook safely within the sandbox.
     */
    private HookResult executeHookSafely(String pluginId, PluginHook hook, HookType hookType,
                                         HookContext context) {
        return sandbox.executeHook(pluginId, hook, hookType, context);
    }

    /**
     * Record hook execution for history tracking.
     */
    private void recordHookExecution(String pluginId, HookResult result) {
        hookHistory.computeIfAbsent(pluginId, k -> new CopyOnWriteArrayList<>()).add(result);

        if (hookHistory.get(pluginId).size() > MAX_HISTORY_SIZE) {
            hookHistory.get(pluginId).remove(0);
        }
    }

    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * Invoke a hook asynchronously for all enabled plugins.
     */
    public CompletableFuture<List<HookResult>> invokeHookAsync(HookType hookType, String projectId,
                                                                Map<String, Object> payload) {
        return CompletableFuture.supplyAsync(() -> invokeHook(hookType, projectId, payload), asyncExecutor);
    }

    /**
     * Invoke a single plugin's hook for testing purposes.
     */
    public HookResult invokePluginHook(String pluginId, HookType hookType, Map<String, Object> payload) {
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        PluginHook hook = hooks.get(pluginId);
        if (hook == null) {
            throw new IllegalStateException("Plugin has no hooks: " + pluginId);
        }

        HookContext context = new HookContext(hookType, manifest.getProjectId(), pluginId, payload);
        return executeHookSafely(pluginId, hook, hookType, context);
    }

    /**
     * Test plugin execution with a sample hook invocation.
     */
    public HookResult testPlugin(String pluginId) {
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        PluginHook hook = hooks.get(pluginId);
        if (hook == null) {
            throw new IllegalStateException("Plugin has no hooks to test: " + pluginId);
        }

        Map<String, Object> testPayload = new HashMap<>();
        testPayload.put("test", true);
        testPayload.put("timestamp", System.currentTimeMillis());
        testPayload.put("testId", "test-" + System.currentTimeMillis());
        testPayload.put("testName", "Plugin Test");

        HookContext context = new HookContext(
                HookType.TEST_CREATED,
                manifest.getProjectId(),
                pluginId,
                testPayload
        );

        return executeHookSafely(pluginId, hook, HookType.TEST_CREATED, context);
    }

    /**
     * Test a specific hook type for a plugin.
     */
    public HookResult testPluginHook(String pluginId, HookType hookType) {
        PluginManifest manifest = manifests.get(pluginId);
        if (manifest == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        PluginHook hook = hooks.get(pluginId);
        if (hook == null) {
            throw new IllegalStateException("Plugin has no hooks: " + pluginId);
        }

        Set<HookType> implementedHooks = getPluginHooks(pluginId);
        if (!implementedHooks.contains(hookType)) {
            throw new IllegalStateException("Plugin does not implement hook type: " + hookType);
        }

        Map<String, Object> testPayload = new HashMap<>();
        testPayload.put("testMode", true);
        testPayload.put("timestamp", System.currentTimeMillis());

        HookContext context = new HookContext(hookType, manifest.getProjectId(), pluginId, testPayload);
        return executeHookSafely(pluginId, hook, hookType, context);
    }

    /**
     * Aggregate results from multiple hook executions.
     */
    public HookAggregationResult aggregateResults(List<HookResult> results) {
        if (results == null || results.isEmpty()) {
            return new HookAggregationResult(0, 0, 0, Collections.emptyList());
        }

        int total = results.size();
        int successCount = (int) results.stream().filter(HookResult::isSuccess).count();
        int failureCount = total - successCount;

        List<HookResult> failures = results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());

        return new HookAggregationResult(total, successCount, failureCount, failures);
    }

    /**
     * Hook chaining - execute hooks in sequence, passing results to next hook.
     */
    public List<HookResult> invokeHookChain(HookType hookType, String projectId, Map<String, Object> payload) {
        List<String> subscribers = hookSubscriptions.get(hookType);
        if (subscribers == null || subscribers.isEmpty()) {
            return Collections.emptyList();
        }

        List<HookResult> results = new ArrayList<>();
        Map<String, Object> chainPayload = new HashMap<>(payload);

        for (String pluginId : subscribers) {
            PluginManifest manifest = manifests.get(pluginId);
            if (manifest == null || !manifest.getEnabled()) {
                continue;
            }

            if (!projectId.equals(manifest.getProjectId())) {
                continue;
            }

            PluginHook hook = hooks.get(pluginId);
            if (hook == null) {
                continue;
            }

            HookContext context = new HookContext(hookType, projectId, pluginId, chainPayload);

            try {
                HookResult result = executeHookSafely(pluginId, hook, hookType, context);
                results.add(result);

                if (result.isSuccess() && result.getData() != null) {
                    chainPayload.putAll(result.getData());
                }
            } catch (Exception e) {
                results.add(HookResult.failure("Chain execution failed: " + e.getMessage()));
                break;
            }
        }

        return results;
    }

    private void registerHookTypes(String pluginId, HookType[] hookTypes) {
        if (hookTypes == null || hookTypes.length == 0) {
            return;
        }

        Set<HookType> types = new HashSet<>(Arrays.asList(hookTypes));
        pluginHooks.put(pluginId, types);

        for (HookType type : hookTypes) {
            List<String> subscribers = hookSubscriptions.get(type);
            if (subscribers != null && !subscribers.contains(pluginId)) {
                subscribers.add(pluginId);
            }
        }
    }

    private Map<String, String> extractConfig(PluginManifest manifest) {
        Map<String, String> config = new HashMap<>();
        config.put("pluginId", manifest.getPluginId());
        config.put("name", manifest.getName());
        config.put("version", manifest.getVersion());
        config.put("author", manifest.getAuthor());
        config.put("vendor", manifest.getVendor());
        config.put("projectId", manifest.getProjectId());
        return config;
    }

    /**
     * Start periodic cleanup of hook history.
     */
    private void startHookHistoryCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            if (shuttingDown) return;

            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            log.debug("Cleaning up hook history older than {}", cutoff);

            hookHistory.values().forEach(list -> {
                synchronized (list) {
                    list.removeIf(result -> true);
                }
            });
        }, 1, 24, TimeUnit.HOURS);
    }

    /**
     * Get hook execution history for a plugin.
     */
    public List<HookResult> getHookHistory(String pluginId) {
        List<HookResult> history = hookHistory.get(pluginId);
        return history != null ? new ArrayList<>(history) : Collections.emptyList();
    }

    /**
     * Get plugin statistics.
     */
    public PluginStats getStats() {
        int total = manifests.size();
        int enabled = (int) manifests.values().stream()
                .filter(PluginManifest::getEnabled)
                .count();
        int error = (int) manifests.values().stream()
                .filter(m -> m.getStatus() == PluginStatus.ERROR)
                .count();

        long totalHookExecutions = hookHistory.values().stream()
                .mapToLong(List::size)
                .sum();

        long totalHookFailures = hookHistory.values().stream()
                .mapToLong(list -> list.stream().filter(r -> !r.isSuccess()).count())
                .sum();

        return new PluginStats(total, enabled, error, totalHookExecutions, totalHookFailures);
    }

    /**
     * Get all dependencies for a plugin.
     */
    public Set<String> getDependencies(String pluginId) {
        Set<String> deps = pluginDependencies.get(pluginId);
        return deps != null ? new HashSet<>(deps) : Collections.emptySet();
    }

    /**
     * Get version information for a plugin.
     */
    public PluginVersion getVersion(String pluginId) {
        return pluginVersions.get(pluginId);
    }

    /**
     * Get count of registered plugins.
     */
    public int getPluginCount() {
        return manifests.size();
    }

    /**
     * Get count of enabled plugins.
     */
    public int getEnabledPluginCount() {
        return (int) manifests.values().stream()
                .filter(PluginManifest::getEnabled)
                .count();
    }

    /**
     * Check if a plugin is registered.
     */
    public boolean isRegistered(String pluginId) {
        return manifests.containsKey(pluginId);
    }

    /**
     * Check if a plugin is enabled.
     */
    public boolean isEnabled(String pluginId) {
        PluginManifest manifest = manifests.get(pluginId);
        return manifest != null && manifest.getEnabled();
    }

    /**
     * Get all plugins with a specific hook type.
     */
    public List<PluginManifest> getPluginsWithHook(HookType hookType) {
        List<String> subscribers = hookSubscriptions.get(hookType);
        if (subscribers == null) {
            return Collections.emptyList();
        }

        return subscribers.stream()
                .map(manifests::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // Inner classes for results and statistics

    public static class HookAggregationResult {
        private final int total;
        private final int successCount;
        private final int failureCount;
        private final List<HookResult> failures;

        public HookAggregationResult(int total, int successCount, int failureCount, List<HookResult> failures) {
            this.total = total;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.failures = failures;
        }

        public int getTotal() { return total; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<HookResult> getFailures() { return failures; }
        public boolean isAllSuccessful() { return failureCount == 0; }
        public double getSuccessRate() { return total > 0 ? (double) successCount / total : 0.0; }
    }

    public static class PluginStats {
        private final int totalPlugins;
        private final int enabledPlugins;
        private final int errorPlugins;
        private final long totalHookExecutions;
        private final long totalHookFailures;

        public PluginStats(int totalPlugins, int enabledPlugins, int errorPlugins,
                          long totalHookExecutions, long totalHookFailures) {
            this.totalPlugins = totalPlugins;
            this.enabledPlugins = enabledPlugins;
            this.errorPlugins = errorPlugins;
            this.totalHookExecutions = totalHookExecutions;
            this.totalHookFailures = totalHookFailures;
        }

        public int getTotalPlugins() { return totalPlugins; }
        public int getEnabledPlugins() { return enabledPlugins; }
        public int getErrorPlugins() { return errorPlugins; }
        public long getTotalHookExecutions() { return totalHookExecutions; }
        public long getTotalHookFailures() { return totalHookFailures; }
        public double getSuccessRate() {
            return totalHookExecutions > 0
                    ? (double) (totalHookExecutions - totalHookFailures) / totalHookExecutions
                    : 0.0;
        }
    }

    public static class PluginVersion {
        private final String pluginId;
        private final String version;
        private final LocalDateTime installedAt;

        public PluginVersion(String pluginId, String version, LocalDateTime installedAt) {
            this.pluginId = pluginId;
            this.version = version;
            this.installedAt = installedAt;
        }

        public String getPluginId() { return pluginId; }
        public String getVersion() { return version; }
        public LocalDateTime getInstalledAt() { return installedAt; }
    }
}
