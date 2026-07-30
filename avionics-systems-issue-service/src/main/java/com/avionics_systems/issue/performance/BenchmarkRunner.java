package com.avionics_systems.issue.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark Runner for Performance Testing
 * Phase 7 - Polish & Performance
 * Provides utilities for running performance benchmarks
 */
@Component
public class BenchmarkRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    /**
     * Run a simple benchmark
     */
    public BenchmarkResult runBenchmark(String name, int iterations, Runnable task) {
        log.info("Starting benchmark: {} with {} iterations", name, iterations);

        // Warmup phase
        for (int i = 0; i < Math.min(10, iterations / 10); i++) {
            task.run();
        }

        // Actual benchmark
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            task.run();
            long end = System.nanoTime();

            long duration = end - start;
            totalTime += duration;
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
        }

        double avgTimeMs = (double) totalTime / iterations / 1_000_000;
        double minTimeMs = (double) minTime / 1_000_000;
        double maxTimeMs = (double) maxTime / 1_000_000;
        double opsPerSecond = avgTimeMs > 0 ? 1_000.0 / avgTimeMs : 0;

        BenchmarkResult result = new BenchmarkResult(
                name,
                iterations,
                avgTimeMs,
                minTimeMs,
                maxTimeMs,
                opsPerSecond
        );

        log.info("Benchmark {} completed: avg={}ms, min={}ms, max={}ms, ops/s={}",
                name, String.format("%.3f", avgTimeMs), String.format("%.3f", minTimeMs),
                String.format("%.3f", maxTimeMs), String.format("%.0f", opsPerSecond));

        return result;
    }

    /**
     * Benchmark with warmup
     */
    public BenchmarkResult runBenchmarkWithWarmup(String name, int warmupIterations,
                                                    int benchmarkIterations, Runnable task) {
        log.info("Starting benchmark with warmup: {} - warmup: {}, iterations: {}",
                name, warmupIterations, benchmarkIterations);

        // Warmup phase
        for (int i = 0; i < warmupIterations; i++) {
            task.run();
        }

        // Actual benchmark
        return runBenchmark(name, benchmarkIterations, task);
    }

    /**
     * Benchmark with throughput measurement
     */
    public ThroughputResult measureThroughput(String name, int durationSeconds, Runnable task)
            throws InterruptedException {
        log.info("Starting throughput benchmark: {} for {} seconds", name, durationSeconds);

        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        long operations = 0;

        while (System.currentTimeMillis() < endTime) {
            task.run();
            operations++;
        }

        double opsPerSecond = (double) operations / durationSeconds;

        ThroughputResult result = new ThroughputResult(name, operations, opsPerSecond, durationSeconds);
        log.info("Throughput benchmark {}: {} ops in {}s = {} ops/s",
                name, operations, durationSeconds, String.format("%.0f", opsPerSecond));

        return result;
    }

    // Result classes
    public record BenchmarkResult(
            String name,
            int iterations,
            double avgTimeMs,
            double minTimeMs,
            double maxTimeMs,
            double opsPerSecond
    ) {}

    public record ThroughputResult(
            String name,
            long totalOperations,
            double opsPerSecond,
            int durationSeconds
    ) {}
}