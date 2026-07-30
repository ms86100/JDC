package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.dto.JobProgressResponse;
import com.avionics_systems.migration.service.MigrationService;
import com.avionics_systems.migration.service.PollingFallbackService;
import com.avionics_systems.migration.websocket.dto.JobProgressUpdate;
import com.avionics_systems.migration.websocket.dto.ValidationUpdate;
import com.avionics_systems.migration.websocket.dto.WebSocketMessage;
import com.avionics_systems.migration.websocket.dto.ImportCompleteNotification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server-Sent Events (SSE) controller for clients that need real-time updates
 * but prefer SSE over WebSocket.
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SSE", description = "Server-Sent Events endpoints for real-time updates")
public class SseController {

    @Value("${sse.timeout-ms:60000}")
    private long sseTimeout;

    @Value("${sse.reconnect-delay-ms:3000}")
    private long reconnectDelay;

    private final MigrationService migrationService;
    private final PollingFallbackService pollingFallbackService;

    // Track active emitters per job
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> jobEmitters = new ConcurrentHashMap<>();

    // Thread pool for async SSE operations
    private final ExecutorService sseExecutor = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors() * 2, 20));

    @GetMapping("/job/{jobId}/stream")
    @Operation(summary = "SSE endpoint for progress streaming")
    public SseEmitter streamProgress(
            @PathVariable String jobId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        SseEmitter emitter = createEmitter(jobId, userId, "progress");

        // Send initial event
        try {
            sendInitialStatus(emitter, jobId);
        } catch (IOException e) {
            log.error("Failed to send initial status for job {}", jobId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping("/job/{jobId}/validation/stream")
    @Operation(summary = "SSE endpoint for validation updates")
    public SseEmitter streamValidation(
            @PathVariable String jobId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId,
            @RequestParam(required = false) String sessionId) {

        SseEmitter emitter = createEmitter(jobId, userId, "validation");

        // Send validation stream info
        try {
            emitter.send(SseEmitter.event()
                    .name("VALIDATION_START")
                    .data(Map.of(
                            "jobId", jobId,
                            "userId", userId,
                            "sessionId", sessionId != null ? sessionId : "default",
                            "timestamp", Instant.now().toString()
                    )));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping("/job/{jobId}/completion/stream")
    @Operation(summary = "SSE endpoint for job completion notification")
    public SseEmitter streamCompletion(
            @PathVariable String jobId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        SseEmitter emitter = createEmitter(jobId, userId, "completion");
        return emitter;
    }

    @PostMapping("/job/{jobId}/emit/progress")
    @Operation(summary = "Internal endpoint to emit progress event to SSE clients")
    public void emitProgressEvent(
            @PathVariable String jobId,
            @RequestBody JobProgressUpdate progressUpdate) {

        broadcastToJob(jobId, "PROGRESS_UPDATE", progressUpdate);
    }

    @PostMapping("/job/{jobId}/emit/validation")
    @Operation(summary = "Internal endpoint to emit validation event to SSE clients")
    public void emitValidationEvent(
            @PathVariable String jobId,
            @RequestBody ValidationUpdate validationUpdate) {

        broadcastToJob(jobId, "VALIDATION_ERROR", validationUpdate);
    }

    @PostMapping("/job/{jobId}/emit/completion")
    @Operation(summary = "Internal endpoint to emit completion event to SSE clients")
    public void emitCompletionEvent(
            @PathVariable String jobId,
            @RequestBody ImportCompleteNotification notification) {

        broadcastToJob(jobId, "JOB_COMPLETED", notification);
    }

    private SseEmitter createEmitter(String jobId, String userId, String streamType) {
        SseEmitter emitter = new SseEmitter(sseTimeout);

        String emitterKey = jobId + ":" + streamType;

        // Register emitter
        jobEmitters.computeIfAbsent(emitterKey, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Send keep-alive heartbeat periodically
        sseExecutor.submit(() -> {
            try {
                Thread.sleep(reconnectDelay);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("HEARTBEAT")
                                .data(Map.of("timestamp", Instant.now().toString())));
                        Thread.sleep(reconnectDelay);
                    } catch (IllegalStateException | IOException ex) {
                        // Emitter completed or error, exit loop
                        break;
                    }
                }
            } catch (InterruptedException e) {
                log.debug("Heartbeat loop interrupted for {}", emitterKey);
                Thread.currentThread().interrupt();
            }
        });

        // Cleanup on completion
        emitter.onCompletion(() -> {
            removeEmitter(emitterKey, emitter);
            log.debug("SSE completed for job {} stream {}", jobId, streamType);
        });

        emitter.onTimeout(() -> {
            removeEmitter(emitterKey, emitter);
            log.debug("SSE timeout for job {} stream {}", jobId, streamType);
        });

        emitter.onError(e -> {
            removeEmitter(emitterKey, emitter);
            log.warn("SSE error for job {} stream {}: {}", jobId, streamType, e.getMessage());
        });

        log.info("SSE emitter created for job {} stream {} (user: {})", jobId, streamType, userId);

        return emitter;
    }

    private void sendInitialStatus(SseEmitter emitter, String jobId) throws IOException {
        // Try cached progress first
        Optional<JobProgressUpdate> cached = pollingFallbackService.getCachedProgress(jobId);

        if (cached.isPresent()) {
            emitter.send(SseEmitter.event()
                    .name("INITIAL_STATUS")
                    .data(cached.get()));
        } else {
            // Try to get from migration service
            try {
                UUID jobUuid = UUID.fromString(jobId);
                JobProgressResponse progress = migrationService.getJobProgress(jobUuid);
                if (progress != null) {
                    JobProgressUpdate update = JobProgressUpdate.fromProgressResponse(progress);
                    pollingFallbackService.cacheProgress(jobId, update);
                    emitter.send(SseEmitter.event()
                            .name("INITIAL_STATUS")
                            .data(update));
                }
            } catch (IllegalArgumentException e) {
                emitter.send(SseEmitter.event()
                        .name("ERROR")
                        .data(Map.of("error", "Invalid job ID format")));
            } catch (IOException e) {
                log.error("Failed to send initial status", e);
            }
        }
    }

    private void broadcastToJob(String jobId, String eventName, Object data) {
        String progressKey = jobId + ":progress";
        CopyOnWriteArrayList<SseEmitter> emitters = jobEmitters.get(progressKey);

        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    log.warn("Failed to send SSE event to emitter for job {}", jobId);
                    emitter.completeWithError(e);
                }
            }
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = jobEmitters.get(key);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                jobEmitters.remove(key);
            }
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get SSE connection statistics")
    public Map<String, Object> getStats() {
        int totalEmitters = jobEmitters.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();

        return Map.of(
                "activeEmitters", totalEmitters,
                "trackedJobs", jobEmitters.size(),
                "timeoutMs", sseTimeout,
                "reconnectDelayMs", reconnectDelay
        );
    }

    @DeleteMapping("/job/{jobId}/close")
    @Operation(summary = "Close all SSE connections for a job")
    public void closeJobConnections(@PathVariable String jobId) {
        String progressKey = jobId + ":progress";
        CopyOnWriteArrayList<SseEmitter> emitters = jobEmitters.remove(progressKey);

        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("STREAM_CLOSED")
                            .data(Map.of("jobId", jobId)));
                    emitter.complete();
                } catch (IOException e) {
                    log.warn("Error closing emitter for job {}", jobId);
                }
            }
        }
    }
}