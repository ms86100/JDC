package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.batch.DeadLetterQueueService;
import com.avionics_systems.migration.entity.DlqEntry;
import com.avionics_systems.migration.repository.DlqEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Dead Letter Queue (DLQ) Management.
 * Provides DLQ operations at /api/migration/dlq
 */
@RestController
@RequestMapping("/api/migration/dlq")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DLQ Management", description = "Dead Letter Queue operations")
public class DlqController {

    private final DeadLetterQueueService deadLetterQueueService;
    private final DlqEntryRepository dlqEntryRepository;

    /**
     * GET /api/migration/dlq - List DLQ entries
     */
    @GetMapping
    @Operation(summary = "List DLQ entries", description = "Returns paginated list of DLQ entries")
    public ResponseEntity<Page<DlqEntry>> listDlqEntries(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.info("Listing DLQ entries: page={}, size={}", page, size);

        Page<DlqEntry> entries = dlqEntryRepository.findAll(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(entries);
    }

    /**
     * GET /api/migration/dlq/pending - List pending DLQ entries
     */
    @GetMapping("/pending")
    @Operation(summary = "List pending DLQ entries", description = "Returns pending DLQ entries for retry")
    public ResponseEntity<List<DlqEntry>> listPendingEntries(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        log.info("Listing pending DLQ entries: page={}, size={}", page, size);

        List<DeadLetterQueueService.FailedOperation> pending =
                deadLetterQueueService.getPending(page, size);

        return ResponseEntity.ok(pending.stream()
                .map(this::toDlqEntry)
                .toList());
    }

    /**
     * GET /api/migration/dlq/{id} - Get DLQ entry by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get DLQ entry", description = "Returns a specific DLQ entry")
    public ResponseEntity<DlqEntry> getDlqEntry(
            @Parameter(description = "DLQ Entry ID") @PathVariable UUID id) {

        log.info("Getting DLQ entry: id={}", id);

        return dlqEntryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/migration/dlq/retry/{id} - Retry failed entry
     */
    @PostMapping("/retry/{id}")
    @Operation(summary = "Retry DLQ entry", description = "Retries a specific DLQ entry")
    public ResponseEntity<DeadLetterQueueService.RetryResult> retryEntry(
            @Parameter(description = "DLQ Entry ID") @PathVariable UUID id) {

        log.info("Retrying DLQ entry: id={}", id);

        try {
            DeadLetterQueueService.RetryResult result =
                    deadLetterQueueService.retry(id.toString());

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(500).body(result);
            }
        } catch (Exception e) {
            log.error("Failed to retry DLQ entry {}: {}", id, e.getMessage());
            return ResponseEntity.status(500)
                    .body(DeadLetterQueueService.RetryResult.builder()
                            .dlqId(id.toString())
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/migration/dlq/retry/all - Retry all pending entries
     */
    @PostMapping("/retry/all")
    @Operation(summary = "Retry all DLQ entries", description = "Retries all pending DLQ entries")
    public ResponseEntity<DeadLetterQueueService.RetrySummary> retryAll() {

        log.info("Retrying all DLQ entries");

        try {
            DeadLetterQueueService.RetrySummary summary = deadLetterQueueService.retryAll();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to retry all DLQ entries: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(DeadLetterQueueService.RetrySummary.builder()
                            .failedCount(0)
                            .errors(List.of(e.getMessage()))
                            .build());
        }
    }

    /**
     * DELETE /api/migration/dlq/{id} - Remove DLQ entry
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete DLQ entry", description = "Removes a DLQ entry from the queue")
    public ResponseEntity<Void> deleteEntry(
            @Parameter(description = "DLQ Entry ID") @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Manual deletion") String reason) {

        log.info("Deleting DLQ entry: id={}, reason={}", id, reason);

        try {
            deadLetterQueueService.discard(id.toString(), reason);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete DLQ entry {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/migration/dlq/purge - Purge completed entries
     */
    @DeleteMapping("/purge")
    @Operation(summary = "Purge completed DLQ entries", description = "Removes all completed/discarded entries")
    public ResponseEntity<Map<String, Object>> purgeCompleted() {

        log.info("Purging completed DLQ entries");

        int deleted = dlqEntryRepository.deleteOldEntries(
                java.time.LocalDateTime.now().minusDays(30));

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "deleted", deleted,
                "message", "Purged " + deleted + " completed DLQ entries"
        ));
    }

    /**
     * GET /api/migration/dlq/statistics - Get DLQ statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get DLQ statistics", description = "Returns DLQ queue statistics")
    public ResponseEntity<DeadLetterQueueService.DLQStatistics> getStatistics() {

        log.info("Getting DLQ statistics");

        try {
            DeadLetterQueueService.DLQStatistics stats = deadLetterQueueService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get DLQ statistics: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * GET /api/migration/dlq/job/{jobId} - Get DLQ entries for a job
     */
    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get DLQ entries for job", description = "Returns DLQ entries for a specific job")
    public ResponseEntity<List<DeadLetterQueueService.FailedOperation>> getByJobId(
            @Parameter(description = "Job ID") @PathVariable UUID jobId) {

        log.info("Getting DLQ entries for job: id={}", jobId);

        List<DeadLetterQueueService.FailedOperation> entries =
                deadLetterQueueService.getByJobId(jobId.toString());

        return ResponseEntity.ok(entries);
    }

    /**
     * Convert FailedOperation to DlqEntry for response.
     * Since FailedOperation uses String ID and DLQ uses UUID, we convert.
     */
    private DlqEntry toDlqEntry(DeadLetterQueueService.FailedOperation op) {
        DlqEntry entry = DlqEntry.builder()
                .operationType(op.getOperationType())
                .entityType(op.getEntityType())
                .entityKey(op.getEntityKey())
                .payload(op.getPayload())
                .errorMessage(op.getErrorMessage())
                .errorStackTrace(op.getErrorStackTrace())
                .attemptCount(op.getAttemptCount())
                .lastError(op.getLastError())
                .status(parseStatus(op.getStatus()))
                .metadata(op.getMetadata())
                .build();

        if (op.getId() != null) {
            try {
                entry.setId(UUID.fromString(op.getId()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (op.getFirstFailure() != null) {
            entry.setFirstFailure(java.time.LocalDateTime.ofInstant(
                    op.getFirstFailure(), java.time.ZoneOffset.UTC));
        }

        if (op.getLastAttempt() != null) {
            entry.setLastAttempt(java.time.LocalDateTime.ofInstant(
                    op.getLastAttempt(), java.time.ZoneOffset.UTC));
        }

        return entry;
    }

    private DlqEntry.DlqStatus parseStatus(String status) {
        if (status == null) return DlqEntry.DlqStatus.PENDING;
        try {
            return DlqEntry.DlqStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return DlqEntry.DlqStatus.PENDING;
        }
    }
}