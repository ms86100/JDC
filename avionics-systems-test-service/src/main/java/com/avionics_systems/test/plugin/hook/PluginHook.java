package com.avionics_systems.test.plugin.hook;

import java.util.Map;

/**
 * Interface defining plugin hook points in the test management system.
 * Plugins implement specific hook types to inject custom behavior at key lifecycle events.
 */
public interface PluginHook {

    /**
     * Hook types available for plugin implementations.
     */
    enum HookType {
        TEST_CREATED("test.created"),
        TEST_EXECUTION_STARTED("test.execution.started"),
        TEST_EXECUTION_COMPLETED("test.execution.completed"),
        PRECONDITION_EVALUATED("precondition.evaluated"),
        COVERAGE_CALCULATED("coverage.calculated");

        private final String eventName;

        HookType(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }
    }

    /**
     * Context passed to hooks when they are invoked.
     */
    class HookContext {
        private final HookType hookType;
        private final String projectId;
        private final String pluginId;
        private final Map<String, Object> payload;
        private final long timestamp;

        public HookContext(HookType hookType, String projectId, String pluginId, Map<String, Object> payload) {
            this.hookType = hookType;
            this.projectId = projectId;
            this.pluginId = pluginId;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
        }

        public HookType getHookType() {
            return hookType;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getPluginId() {
            return pluginId;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Object get(String key) {
            return payload != null ? payload.get(key) : null;
        }

        public <T> T get(String key, Class<T> type) {
            Object value = get(key);
            if (value != null && type.isInstance(value)) {
                return type.cast(value);
            }
            return null;
        }
    }

    /**
     * Result returned from hook execution.
     */
    class HookResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;

        private HookResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static HookResult success() {
            return new HookResult(true, null, null);
        }

        public static HookResult success(String message) {
            return new HookResult(true, message, null);
        }

        public static HookResult success(Map<String, Object> data) {
            return new HookResult(true, null, data);
        }

        public static HookResult success(String message, Map<String, Object> data) {
            return new HookResult(true, message, data);
        }

        public static HookResult failure(String message) {
            return new HookResult(false, message, null);
        }

        public static HookResult failure(String message, Map<String, Object> data) {
            return new HookResult(false, message, data);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    /**
     * Returns the hook types this plugin implements.
     */
    HookType[] getHookTypes();

    /**
     * Called after a test is created.
     * Payload keys: testId, testName, testType, projectId, createdBy, createdAt
     */
    default HookResult onTestCreated(HookContext context) {
        return HookResult.success();
    }

    /**
     * Called before test execution starts.
     * Payload keys: executionId, testId, projectId, environment, triggeredBy
     */
    default HookResult onTestExecutionStarted(HookContext context) {
        return HookResult.success();
    }

    /**
     * Called after test execution completes.
     * Payload keys: executionId, testId, result, duration, projectId, environment
     */
    default HookResult onTestExecutionCompleted(HookContext context) {
        return HookResult.success();
    }

    /**
     * Called after precondition evaluation.
     * Payload keys: preconditionId, testId, result, evaluatedAt, projectId
     */
    default HookResult onPreconditionEvaluated(HookContext context) {
        return HookResult.success();
    }

    /**
     * Called after coverage calculation.
     * Payload keys: projectId, coveragePercentage, coveredItems, totalItems, calculatedAt
     */
    default HookResult onCoverageCalculated(HookContext context) {
        return HookResult.success();
    }

    /**
     * Initialize the hook with configuration.
     */
    default void initialize(Map<String, String> config) {
    }

    /**
     * Cleanup resources when plugin is unloaded.
     */
    default void destroy() {
    }
}
