package com.jira.migration.batch.event;

import com.jira.migration.batch.JobState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Event publisher for migration events.
 * Supports both Spring ApplicationEventPublisher and custom listeners.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    // Custom event listeners (for non-Spring contexts)
    private final Map<String, CopyOnWriteArrayList<Consumer<MigrationEvent>>> eventListeners = new ConcurrentHashMap<>();

    /**
     * Publish a job started event.
     */
    public void publishJobStarted(String jobId, String jobType, String source, Map<String, Object> options) {
        JobStartedEvent event = JobStartedEvent.builder()
                .jobId(jobId)
                .jobType(jobType)
                .source(source)
                .timestamp(Instant.now())
                .options(options)
                .build();
        publish(event);
    }

    /**
     * Publish a job completed event.
     */
    public void publishJobCompleted(String jobId, int successCount, int failCount,
                                    long durationMs, Map<String, Object> resultMetadata) {
        JobCompletedEvent event = JobCompletedEvent.builder()
                .jobId(jobId)
                .successCount(successCount)
                .failCount(failCount)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .resultMetadata(resultMetadata)
                .build();
        publish(event);
    }

    /**
     * Publish a job failed event.
     */
    public void publishJobFailed(String jobId, String error, String errorCode, String stackTrace) {
        JobFailedEvent event = JobFailedEvent.builder()
                .jobId(jobId)
                .error(error)
                .errorCode(errorCode)
                .timestamp(Instant.now())
                .stackTrace(stackTrace)
                .build();
        publish(event);
    }

    /**
     * Publish a batch started event.
     */
    public void publishBatchStarted(String jobId, int batchNumber, int totalBatches,
                                     int batchSize, String entityType) {
        BatchStartedEvent event = BatchStartedEvent.builder()
                .jobId(jobId)
                .batchNumber(batchNumber)
                .totalBatches(totalBatches)
                .batchSize(batchSize)
                .entityType(entityType)
                .timestamp(Instant.now())
                .build();
        publish(event);
    }

    /**
     * Publish a batch completed event.
     */
    public void publishBatchCompleted(String jobId, int batchNumber, int successCount,
                                       int errorCount, long durationMs) {
        BatchCompletedEvent event = BatchCompletedEvent.builder()
                .jobId(jobId)
                .batchNumber(batchNumber)
                .successCount(successCount)
                .errorCount(errorCount)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
        publish(event);
    }

    /**
     * Publish an entity imported event.
     */
    public void publishEntityImported(String jobId, String entityType, String entityId,
                                       String entityKey, String targetId) {
        EntityImportedEvent event = EntityImportedEvent.builder()
                .jobId(jobId)
                .entityType(entityType)
                .entityId(entityId)
                .entityKey(entityKey)
                .targetId(targetId)
                .timestamp(Instant.now())
                .build();
        publish(event);
    }

    /**
     * Publish an import progress event.
     */
    public void publishImportProgress(String jobId, int percentage, int processedCount,
                                       int totalCount, int successCount, int errorCount,
                                       long estimatedRemainingMs) {
        ImportProgressEvent event = ImportProgressEvent.builder()
                .jobId(jobId)
                .percentage(percentage)
                .processedCount(processedCount)
                .totalCount(totalCount)
                .successCount(successCount)
                .errorCount(errorCount)
                .estimatedRemainingMs(estimatedRemainingMs)
                .timestamp(Instant.now())
                .build();
        publish(event);
    }

    /**
     * Publish a custom migration event.
     */
    public void publish(MigrationEvent event) {
        log.debug("Publishing event: {} for job {}", event.getEventType(), event.getJobId());

        // Publish to Spring ApplicationEventPublisher
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish to Spring event publisher: {}", e.getMessage());
        }

        // Publish to custom listeners
        CopyOnWriteArrayList<Consumer<MigrationEvent>> listeners = eventListeners.get(event.getEventType());
        if (listeners != null) {
            for (Consumer<MigrationEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    log.warn("Custom listener failed for event {}: {}", event.getEventType(), e.getMessage());
                }
            }
        }
    }

    /**
     * Register a listener for a specific event type.
     */
    public void registerListener(String eventType, Consumer<MigrationEvent> listener) {
        eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("Registered listener for event type: {}", eventType);
    }

    /**
     * Unregister a listener for a specific event type.
     */
    public boolean unregisterListener(String eventType, Consumer<MigrationEvent> listener) {
        CopyOnWriteArrayList<Consumer<MigrationEvent>> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            return listeners.remove(listener);
        }
        return false;
    }

    /**
     * Register a listener for all event types.
     */
    public void registerGlobalListener(Consumer<MigrationEvent> listener) {
        // Add to all existing event types
        for (String eventType : eventListeners.keySet()) {
            eventListeners.get(eventType).add(listener);
        }
        // Also add to "ALL" category
        eventListeners.computeIfAbsent("ALL", k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Clear all listeners for an event type.
     */
    public void clearListeners(String eventType) {
        eventListeners.remove(eventType);
    }

    /**
     * Clear all listeners.
     */
    public void clearAllListeners() {
        eventListeners.clear();
    }
}