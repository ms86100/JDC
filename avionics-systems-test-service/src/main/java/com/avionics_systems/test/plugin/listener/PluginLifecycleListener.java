package com.avionics_systems.test.plugin.listener;

import com.avionics_systems.test.plugin.entity.PluginManifest;
import com.avionics_systems.test.plugin.entity.PluginManifest.PluginStatus;
import com.avionics_systems.test.plugin.service.PluginRegistry;
import com.avionics_systems.test.plugin.service.PluginRegistry.PluginStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring component that listens for application lifecycle events
 * and manages plugin initialization, auto-enabling, and status reporting.
 */
@Component
@Slf4j
public class PluginLifecycleListener {

    private final PluginRegistry pluginRegistry;
    private final AtomicInteger startupPluginsEnabled = new AtomicInteger(0);
    private final AtomicInteger totalHookInvocations = new AtomicInteger(0);
    private volatile boolean applicationReady = false;

    public PluginLifecycleListener(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        log.info("PluginLifecycleListener initialized");
    }

    /**
     * Handle ApplicationReadyEvent to auto-enable installed plugins.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Application ready event received - initializing plugins");
        applicationReady = true;

        try {
            List<PluginManifest> installedPlugins = pluginRegistry.getAllPlugins();

            log.info("Found {} installed plugins", installedPlugins.size());

            int enabled = 0;
            int skipped = 0;
            int errors = 0;

            for (PluginManifest plugin : installedPlugins) {
                try {
                    if (shouldAutoEnable(plugin)) {
                        enablePlugin(plugin);
                        enabled++;
                    } else {
                        skipped++;
                        log.debug("Plugin {} skipped auto-enable - status: {}",
                                plugin.getPluginId(), plugin.getStatus());
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("Failed to auto-enable plugin {}: {}",
                            plugin.getPluginId(), e.getMessage());
                }
            }

            startupPluginsEnabled.set(enabled);
            logPluginStatus(installedPlugins);
            log.info("Plugin auto-initialization complete: enabled={}, skipped={}, errors={}",
                    enabled, skipped, errors);

        } catch (Exception e) {
            log.error("Error during plugin auto-initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Determine if a plugin should be auto-enabled at startup.
     */
    private boolean shouldAutoEnable(PluginManifest plugin) {
        if (plugin == null) {
            return false;
        }

        PluginStatus status = plugin.getStatus();
        if (status == PluginStatus.ERROR) {
            return false;
        }

        if (status == PluginStatus.ENABLED) {
            return true;
        }

        if (status == PluginStatus.INSTALLED) {
            return true;
        }

        if (status == PluginStatus.PENDING) {
            return true;
        }

        return false;
    }

    /**
     * Enable a single plugin with proper error handling.
     */
    private void enablePlugin(PluginManifest plugin) {
        String pluginId = plugin.getPluginId();
        log.debug("Auto-enabling plugin: {}", pluginId);

        try {
            pluginRegistry.enablePlugin(pluginId);
            log.info("Plugin auto-enabled: {} v{}", pluginId, plugin.getVersion());
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("dependency")) {
                log.warn("Plugin {} has unresolved dependencies - not auto-enabled", pluginId);
            } else {
                throw e;
            }
        }
    }

    /**
     * Log comprehensive plugin status summary.
     */
    private void logPluginStatus(List<PluginManifest> plugins) {
        if (plugins.isEmpty()) {
            log.info("No plugins installed");
            return;
        }

        PluginStats stats = pluginRegistry.getStats();
        log.info("Plugin Status Summary:");
        log.info("  Total Plugins: {}", stats.getTotalPlugins());
        log.info("  Enabled: {}", stats.getEnabledPlugins());
        log.info("  Errors: {}", stats.getErrorPlugins());
        log.info("  Hook Executions: {}", stats.getTotalHookExecutions());
        log.info("  Hook Success Rate: {}%", String.format("%.2f", stats.getSuccessRate() * 100));

        log.info("Installed Plugins:");
        for (PluginManifest plugin : plugins) {
            log.info("  - {} v{} [{}] {}",
                    plugin.getName(),
                    plugin.getVersion(),
                    plugin.getStatus(),
                    plugin.getEnabled() ? "(enabled)" : "(disabled)");
        }
    }

    /**
     * Handle shutdown to gracefully disable all plugins.
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Application shutting down - disabling all plugins");
        applicationReady = false;

        try {
            List<PluginManifest> enabledPlugins = pluginRegistry.getAllPlugins().stream()
                    .filter(PluginManifest::getEnabled)
                    .toList();

            for (PluginManifest plugin : enabledPlugins) {
                try {
                    pluginRegistry.disablePlugin(plugin.getPluginId());
                    log.debug("Plugin disabled during shutdown: {}", plugin.getPluginId());
                } catch (Exception e) {
                    log.warn("Error disabling plugin {} during shutdown: {}",
                            plugin.getPluginId(), e.getMessage());
                }
            }

            log.info("Shutdown complete - {} plugins disabled", enabledPlugins.size());

        } catch (Exception e) {
            log.error("Error during plugin shutdown: {}", e.getMessage(), e);
        }
    }

    /**
     * Get count of plugins enabled at startup.
     */
    public int getStartupPluginsEnabled() {
        return startupPluginsEnabled.get();
    }

    /**
     * Check if application is ready and plugins are initialized.
     */
    public boolean isApplicationReady() {
        return applicationReady;
    }

    /**
     * Get comprehensive plugin status report.
     */
    public PluginStatusReport getStatusReport() {
        PluginStats stats = pluginRegistry.getStats();
        List<PluginManifest> allPlugins = pluginRegistry.getAllPlugins();

        return new PluginStatusReport(
                applicationReady,
                LocalDateTime.now(),
                stats.getTotalPlugins(),
                stats.getEnabledPlugins(),
                stats.getErrorPlugins(),
                stats.getTotalHookExecutions(),
                stats.getTotalHookFailures(),
                stats.getSuccessRate(),
                startupPluginsEnabled.get(),
                allPlugins.stream()
                        .filter(PluginManifest::getEnabled)
                        .map(PluginManifest::getPluginId)
                        .toList(),
                allPlugins.stream()
                        .filter(p -> p.getStatus() == PluginStatus.ERROR)
                        .map(PluginManifest::getPluginId)
                        .toList()
        );
    }

    /**
     * Record a hook invocation for tracking.
     */
    public void recordHookInvocation() {
        totalHookInvocations.incrementAndGet();
    }

    /**
     * Get total hook invocations since startup.
     */
    public int getTotalHookInvocations() {
        return totalHookInvocations.get();
    }

    /**
     * Inner class for status report.
     */
    public static class PluginStatusReport {
        private final boolean applicationReady;
        private final LocalDateTime timestamp;
        private final int totalPlugins;
        private final int enabledPlugins;
        private final int errorPlugins;
        private final long totalHookExecutions;
        private final long totalHookFailures;
        private final double successRate;
        private final int startupPluginsEnabled;
        private final List<String> enabledPluginIds;
        private final List<String> errorPluginIds;

        public PluginStatusReport(boolean applicationReady, LocalDateTime timestamp,
                                 int totalPlugins, int enabledPlugins, int errorPlugins,
                                 long totalHookExecutions, long totalHookFailures,
                                 double successRate, int startupPluginsEnabled,
                                 List<String> enabledPluginIds, List<String> errorPluginIds) {
            this.applicationReady = applicationReady;
            this.timestamp = timestamp;
            this.totalPlugins = totalPlugins;
            this.enabledPlugins = enabledPlugins;
            this.errorPlugins = errorPlugins;
            this.totalHookExecutions = totalHookExecutions;
            this.totalHookFailures = totalHookFailures;
            this.successRate = successRate;
            this.startupPluginsEnabled = startupPluginsEnabled;
            this.enabledPluginIds = enabledPluginIds;
            this.errorPluginIds = errorPluginIds;
        }

        public boolean isApplicationReady() { return applicationReady; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getTotalPlugins() { return totalPlugins; }
        public int getEnabledPlugins() { return enabledPlugins; }
        public int getErrorPlugins() { return errorPlugins; }
        public long getTotalHookExecutions() { return totalHookExecutions; }
        public long getTotalHookFailures() { return totalHookFailures; }
        public double getSuccessRate() { return successRate; }
        public int getStartupPluginsEnabled() { return startupPluginsEnabled; }
        public List<String> getEnabledPluginIds() { return enabledPluginIds; }
        public List<String> getErrorPluginIds() { return errorPluginIds; }
    }
}