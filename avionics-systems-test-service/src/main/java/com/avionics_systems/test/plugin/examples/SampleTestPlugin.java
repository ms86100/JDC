package com.avionics_systems.test.plugin.examples;

import com.avionics_systems.test.plugin.hook.PluginHook;
import com.avionics_systems.test.plugin.hook.PluginHook.HookContext;
import com.avionics_systems.test.plugin.hook.PluginHook.HookResult;
import com.avionics_systems.test.plugin.hook.PluginHook.HookType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sample test plugin demonstrating hook implementation, custom REST endpoint,
 * and test listener functionality.
 *
 * This plugin:
 * - Implements multiple hook types
 * - Tracks test lifecycle events
 * - Provides custom data enrichment
 * - Demonstrates proper hook result handling
 */
@Slf4j
public class SampleTestPlugin implements PluginHook {

    private Map<String, String> config;
    private TestEventListener eventListener;
    private boolean initialized = false;

    @Override
    public HookType[] getHookTypes() {
        return new HookType[]{
                HookType.TEST_CREATED,
                HookType.TEST_EXECUTION_STARTED,
                HookType.TEST_EXECUTION_COMPLETED,
                HookType.COVERAGE_CALCULATED
        };
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.config = new HashMap<>(config);
        this.eventListener = new TestEventListener();
        this.initialized = true;

        log.info("SampleTestPlugin initialized - pluginId: {}, version: {}",
                config.get("pluginId"), config.get("version"));
    }

    @Override
    public void destroy() {
        if (eventListener != null) {
            eventListener.clearHistory();
        }
        this.config = null;
        this.initialized = false;
        log.info("SampleTestPlugin destroyed");
    }

    /**
     * Hook called when a test is created.
     * Enriches test metadata and validates test data.
     */
    @Override
    public HookResult onTestCreated(HookContext context) {
        if (!initialized) {
            return HookResult.failure("Plugin not initialized");
        }

        String testId = (String) context.get("testId");
        String testName = (String) context.get("testName");
        String testType = (String) context.get("testType");

        log.debug("Test created: {} - {} ({})", testId, testName, testType);

        if (testId == null || testId.isBlank()) {
            return HookResult.failure("Test ID is required");
        }

        if (testName == null || testName.isBlank()) {
            return HookResult.failure("Test name is required");
        }

        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("pluginEnriched", true);
        enrichedData.put("enrichmentTimestamp", System.currentTimeMillis());
        enrichedData.put("pluginVersion", config.get("version"));
        enrichedData.put("testId", testId);

        eventListener.recordEvent("TEST_CREATED", testId);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("enrichmentApplied", true);
        resultData.put("validationPassed", true);
        resultData.put("metadata", enrichedData);

        return HookResult.success("Test enriched and validated successfully", resultData);
    }

    /**
     * Hook called before test execution starts.
     * Prepares execution environment and validates prerequisites.
     */
    @Override
    public HookResult onTestExecutionStarted(HookContext context) {
        if (!initialized) {
            return HookResult.failure("Plugin not initialized");
        }

        String executionId = (String) context.get("executionId");
        String testId = (String) context.get("testId");
        String environment = (String) context.get("environment");
        String triggeredBy = (String) context.get("triggeredBy");

        log.info("Test execution started: execution={}, test={}, env={}, by={}",
                executionId, testId, environment, triggeredBy);

        if (environment == null || environment.isBlank()) {
            return HookResult.failure("Environment must be specified for execution");
        }

        Map<String, Object> preExecutionData = new HashMap<>();
        preExecutionData.put("environment", environment);
        preExecutionData.put("precheckCompleted", true);
        preExecutionData.put("startTime", System.currentTimeMillis());
        preExecutionData.put("executionId", executionId);

        eventListener.recordEvent("EXECUTION_STARTED", testId);

        return HookResult.success("Pre-execution checks passed", preExecutionData);
    }

    /**
     * Hook called after test execution completes.
     * Records results and performs post-execution analysis.
     */
    @Override
    public HookResult onTestExecutionCompleted(HookContext context) {
        if (!initialized) {
            return HookResult.failure("Plugin not initialized");
        }

        String executionId = (String) context.get("executionId");
        String testId = (String) context.get("testId");
        String result = (String) context.get("result");
        Long duration = context.get("duration", Long.class);

        log.info("Test execution completed: execution={}, test={}, result={}, duration={}ms",
                executionId, testId, result, duration);

        eventListener.recordEvent("EXECUTION_COMPLETED", testId);

        Map<String, Object> analysisData = new HashMap<>();
        analysisData.put("analysisCompleted", true);
        analysisData.put("resultRecorded", true);
        analysisData.put("completionTime", System.currentTimeMillis());
        analysisData.put("executionId", executionId);

        if ("PASSED".equalsIgnoreCase(result)) {
            analysisData.put("trendAnalysis", "Positive outcome recorded");
        } else if ("FAILED".equalsIgnoreCase(result)) {
            analysisData.put("trendAnalysis", "Failure recorded - may need attention");
        }

        return HookResult.success("Post-execution analysis completed", analysisData);
    }

    /**
     * Hook called after coverage calculation.
     * Enriches coverage data with additional metrics.
     */
    @Override
    public HookResult onCoverageCalculated(HookContext context) {
        if (!initialized) {
            return HookResult.failure("Plugin not initialized");
        }

        String projectId = context.getProjectId();
        Double coveragePercentage = context.get("coveragePercentage", Double.class);
        Integer totalItems = context.get("totalItems", Integer.class);
        Integer coveredItems = context.get("coveredItems", Integer.class);

        log.debug("Coverage calculated: project={}, coverage={}%, total={}, covered={}",
                projectId, coveragePercentage, totalItems, coveredItems);

        Map<String, Object> enhancedCoverage = new HashMap<>();
        enhancedCoverage.put("coverageEnriched", true);
        enhancedCoverage.put("enrichmentSource", "SampleTestPlugin");
        enhancedCoverage.put("coveragePercentage", coveragePercentage);
        enhancedCoverage.put("qualityScore", calculateQualityScore(coveragePercentage));

        eventListener.recordEvent("COVERAGE_CALCULATED", projectId);

        return HookResult.success("Coverage data enriched", enhancedCoverage);
    }

    /**
     * Calculate a quality score based on coverage percentage.
     */
    private double calculateQualityScore(Double coverage) {
        if (coverage == null) return 0.0;
        if (coverage >= 90) return 10.0;
        if (coverage >= 80) return 9.0;
        if (coverage >= 70) return 8.0;
        if (coverage >= 60) return 7.0;
        if (coverage >= 50) return 6.0;
        if (coverage >= 40) return 5.0;
        if (coverage >= 30) return 4.0;
        if (coverage >= 20) return 3.0;
        if (coverage >= 10) return 2.0;
        return 1.0;
    }

    /**
     * Get the plugin's event listener for external access.
     */
    public TestEventListener getEventListener() {
        return eventListener;
    }

    /**
     * Check if the plugin is properly initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get plugin configuration.
     */
    public Map<String, String> getConfig() {
        return config != null ? new HashMap<>(config) : null;
    }

    /**
     * Inner class for tracking test events.
     */
    public static class TestEventListener {
        private final Map<String, java.util.List<EventRecord>> eventHistory = new java.util.concurrent.ConcurrentHashMap<>();

        public void recordEvent(String eventType, String targetId) {
            EventRecord record = new EventRecord(eventType, targetId, System.currentTimeMillis());
            eventHistory.computeIfAbsent(targetId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(record);
        }

        public java.util.List<EventRecord> getEvents(String targetId) {
            return new java.util.ArrayList<>(eventHistory.getOrDefault(targetId, java.util.Collections.emptyList()));
        }

        public int getEventCount(String targetId) {
            return eventHistory.getOrDefault(targetId, java.util.Collections.emptyList()).size();
        }

        public void clearHistory() {
            eventHistory.clear();
        }

        public Set<String> getAllTrackedTargets() {
            return eventHistory.keySet();
        }

        public static class EventRecord {
            private final String eventType;
            private final String targetId;
            private final long timestamp;

            public EventRecord(String eventType, String targetId, long timestamp) {
                this.eventType = eventType;
                this.targetId = targetId;
                this.timestamp = timestamp;
            }

            public String getEventType() { return eventType; }
            public String getTargetId() { return targetId; }
            public long getTimestamp() { return timestamp; }
        }
    }
}