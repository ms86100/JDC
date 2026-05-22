package com.jira.test.plugin.hook;

import com.jira.test.plugin.hook.PluginHook.HookContext;
import com.jira.test.plugin.hook.PluginHook.HookResult;
import com.jira.test.plugin.hook.PluginHook.HookType;
import com.jira.test.plugin.sandbox.PluginSandbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Executor for plugin hooks with support for synchronous, asynchronous execution,
 * timeout handling, error isolation, and hook chaining.
 */
@Component
@Slf4j
public class PluginHookExecutor {

    private final PluginSandbox sandbox;
    private final ExecutorService syncExecutor;
    private final ExecutorService asyncExecutor;
    private final ScheduledExecutorService scheduler;

    private static final long DEFAULT_TIMEOUT_MS = 30_000;
    private static final int MAX_CONCURRENT_HOOKS = 20;

    public PluginHookExecutor(PluginSandbox sandbox) {
        this.sandbox = sandbox;
        this.syncExecutor = Executors.newFixedThreadPool(4);
        this.asyncExecutor = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * Execute a hook synchronously with default timeout.
     */
    public HookResult executeSync(PluginHook hook, HookType hookType, HookContext context) {
        return executeSync(hook, hookType, context, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute a hook synchronously with custom timeout.
     */
    public HookResult executeSync(PluginHook hook, HookType hookType, HookContext context, long timeoutMs) {
        String pluginId = context.getPluginId();

        try {
            Future<HookResult> future = syncExecutor.submit(() ->
                    invokeHook(hook, hookType, context)
            );

            HookResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            log.debug("Hook {} executed successfully for plugin {} in {}ms",
                    hookType, pluginId, System.currentTimeMillis() - context.getTimestamp());

            return result;

        } catch (TimeoutException e) {
            log.warn("Hook {} timed out for plugin {} after {}ms", hookType, pluginId, timeoutMs);
            return HookResult.failure("Hook execution timed out after " + timeoutMs + "ms");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Hook {} execution interrupted for plugin {}", hookType, pluginId);
            return HookResult.failure("Hook execution was interrupted");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("Hook {} failed for plugin {}: {}", hookType, pluginId, cause.getMessage());
            return HookResult.failure("Hook execution failed: " + cause.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error executing hook {} for plugin {}: {}",
                    hookType, pluginId, e.getMessage());
            return HookResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Execute a hook asynchronously.
     */
    public CompletableFuture<HookResult> executeAsync(PluginHook hook, HookType hookType, HookContext context) {
        return executeAsync(hook, hookType, context, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute a hook asynchronously with custom timeout and completion callback.
     */
    public CompletableFuture<HookResult> executeAsync(PluginHook hook, HookType hookType,
                                                       HookContext context, long timeoutMs) {
        CompletableFuture<HookResult> future = new CompletableFuture<>();

        asyncExecutor.submit(() -> {
            try {
                HookResult result = executeSync(hook, hookType, context, timeoutMs);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Execute a hook asynchronously with a callback.
     */
    public void executeAsyncWithCallback(PluginHook hook, HookType hookType, HookContext context,
                                         Consumer<HookResult> callback) {
        executeAsync(hook, hookType, context).whenComplete((result, error) -> {
            if (error != null) {
                callback.accept(HookResult.failure("Async execution failed: " + error.getMessage()));
            } else {
                callback.accept(result);
            }
        });
    }

    /**
     * Execute hooks for multiple plugins synchronously - each in its own thread.
     */
    public List<HookResult> executeMultipleSync(List<PluginHookExecution> executions) {
        return executeMultipleSync(executions, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute hooks for multiple plugins with custom timeout per hook.
     */
    public List<HookResult> executeMultipleSync(List<PluginHookExecution> executions, long timeoutMs) {
        if (executions == null || executions.isEmpty()) {
            return Collections.emptyList();
        }

        CountDownLatch latch = new CountDownLatch(Math.min(executions.size(), MAX_CONCURRENT_HOOKS));
        List<HookResult> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger index = new AtomicInteger(0);

        int numThreads = Math.min(executions.size(), MAX_CONCURRENT_HOOKS);
        for (int i = 0; i < numThreads; i++) {
            asyncExecutor.submit(() -> {
                while (true) {
                    int currentIndex = index.getAndIncrement();
                    if (currentIndex >= executions.size()) {
                        break;
                    }

                    PluginHookExecution execution = executions.get(currentIndex);
                    HookResult result = executeSync(
                            execution.hook,
                            execution.hookType,
                            execution.context,
                            timeoutMs
                    );
                    results.add(result);
                }
                latch.countDown();
            });
        }

        try {
            latch.await(timeoutMs * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return results;
    }

    /**
     * Execute hooks for multiple plugins asynchronously.
     */
    public List<CompletableFuture<HookResult>> executeMultipleAsync(List<PluginHookExecution> executions) {
        return executeMultipleAsync(executions, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Execute hooks for multiple plugins asynchronously with custom timeout.
     */
    public List<CompletableFuture<HookResult>> executeMultipleAsync(List<PluginHookExecution> executions,
                                                                    long timeoutMs) {
        if (executions == null || executions.isEmpty()) {
            return Collections.emptyList();
        }

        return executions.stream()
                .map(exec -> executeAsync(exec.hook, exec.hookType, exec.context, timeoutMs))
                .collect(Collectors.toList());
    }

    /**
     * Aggregate results from multiple futures.
     */
    public CompletableFuture<List<HookResult>> aggregateResults(List<CompletableFuture<HookResult>> futures) {
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        return allFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Hook chaining - execute hooks in sequence, passing data to next.
     */
    public List<HookResult> executeChain(List<PluginHookExecution> chain) {
        return executeChain(chain, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Hook chaining with custom timeout.
     */
    public List<HookResult> executeChain(List<PluginHookExecution> chain, long timeoutMs) {
        if (chain == null || chain.isEmpty()) {
            return Collections.emptyList();
        }

        List<HookResult> results = new ArrayList<>();
        Map<String, Object> chainData = new HashMap<>();

        for (PluginHookExecution execution : chain) {
            if (execution.context.getPayload() != null) {
                chainData.putAll(execution.context.getPayload());
            }

            HookContext chainContext = new HookContext(
                    execution.hookType,
                    execution.context.getProjectId(),
                    execution.context.getPluginId(),
                    chainData
            );

            HookResult result = executeSync(execution.hook, execution.hookType, chainContext, timeoutMs);
            results.add(result);

            if (!result.isSuccess()) {
                log.warn("Hook chain stopped at {} due to failure",
                        execution.context.getPluginId());
                break;
            }

            if (result.getData() != null) {
                chainData.putAll(result.getData());
            }
        }

        return results;
    }

    /**
     * Schedule a hook to be executed at a fixed rate.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(PluginHook hook, HookType hookType,
                                                   HookContext context, long initialDelay,
                                                   long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(() -> {
            try {
                executeSync(hook, hookType, context);
            } catch (Exception e) {
                log.error("Scheduled hook execution failed: {}", e.getMessage());
            }
        }, initialDelay, period, unit);
    }

    /**
     * Schedule a hook to be executed with fixed delay.
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(PluginHook hook, HookType hookType,
                                                      HookContext context, long initialDelay,
                                                      long delay, TimeUnit unit) {
        return scheduler.scheduleWithFixedDelay(() -> {
            try {
                executeSync(hook, hookType, context);
            } catch (Exception e) {
                log.error("Scheduled hook execution failed: {}", e.getMessage());
            }
        }, initialDelay, delay, unit);
    }

    /**
     * Execute hook within sandbox for security isolation.
     */
    public HookResult executeInSandbox(String pluginId, PluginHook hook,
                                        HookType hookType, HookContext context) {
        return sandbox.executeHook(pluginId, hook, hookType, context);
    }

    /**
     * Execute hook within sandbox asynchronously.
     */
    public CompletableFuture<HookResult> executeInSandboxAsync(String pluginId, PluginHook hook,
                                                               HookType hookType, HookContext context) {
        return CompletableFuture.supplyAsync(() ->
                sandbox.executeHook(pluginId, hook, hookType, context), asyncExecutor
        );
    }

    /**
     * Invoke the appropriate hook method based on type.
     */
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
                log.warn("Unknown hook type: {}", hookType);
                return HookResult.failure("Unknown hook type: " + hookType);
        }
    }

    /**
     * Aggregate multiple hook results.
     */
    public AggregationResult aggregate(List<HookResult> results) {
        if (results == null || results.isEmpty()) {
            return new AggregationResult(0, 0, 0, Collections.emptyList());
        }

        int total = results.size();
        int success = (int) results.stream().filter(HookResult::isSuccess).count();
        int failed = total - success;

        List<HookResult> failures = results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());

        return new AggregationResult(total, success, failed, failures);
    }

    /**
     * Get executor statistics.
     */
    public ExecutorStats getStats() {
        return new ExecutorStats(
                MAX_CONCURRENT_HOOKS,
                DEFAULT_TIMEOUT_MS,
                0,
                0
        );
    }

    /**
     * Shutdown executors gracefully.
     */
    public void shutdown() {
        log.info("Shutting down PluginHookExecutor");

        syncExecutor.shutdown();
        asyncExecutor.shutdown();
        scheduler.shutdown();

        try {
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutor.shutdownNow();
            asyncExecutor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Inner classes for data structures

    public static class PluginHookExecution {
        public final PluginHook hook;
        public final HookType hookType;
        public final HookContext context;

        public PluginHookExecution(PluginHook hook, HookType hookType, HookContext context) {
            this.hook = hook;
            this.hookType = hookType;
            this.context = context;
        }
    }

    public static class AggregationResult {
        private final int total;
        private final int successCount;
        private final int failureCount;
        private final List<HookResult> failures;

        public AggregationResult(int total, int successCount, int failureCount, List<HookResult> failures) {
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

    public static class ExecutorStats {
        private final int maxConcurrentHooks;
        private final long defaultTimeoutMs;
        private final long activeExecutions;
        private final long completedExecutions;

        public ExecutorStats(int maxConcurrentHooks, long defaultTimeoutMs,
                            long activeExecutions, long completedExecutions) {
            this.maxConcurrentHooks = maxConcurrentHooks;
            this.defaultTimeoutMs = defaultTimeoutMs;
            this.activeExecutions = activeExecutions;
            this.completedExecutions = completedExecutions;
        }

        public int getMaxConcurrentHooks() { return maxConcurrentHooks; }
        public long getDefaultTimeoutMs() { return defaultTimeoutMs; }
        public long getActiveExecutions() { return activeExecutions; }
        public long getCompletedExecutions() { return completedExecutions; }
    }
}