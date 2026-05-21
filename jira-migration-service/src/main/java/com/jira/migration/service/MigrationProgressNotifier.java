package com.jira.migration.service;

import com.jira.migration.entity.MigrationJob;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.websocket.MigrationWebSocketHandler;
import com.jira.migration.websocket.dto.JobProgressUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MigrationProgressNotifier {

    private final MigrationJobRepository migrationJobRepository;
    private final MigrationWebSocketHandler webSocketHandler;
    private final PollingFallbackService pollingFallbackService;
    private final MigrationJobLogService jobLogService;

    @Transactional
    public void notifyProgress(UUID jobId, String userId, int processed, int total, int failed,
                               String stage, String entityType, String logMessage) {
        MigrationJob job = migrationJobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setProcessedEntities(processed);
            job.setFailedEntities(failed);
            job.setTotalEntities(total);
            job.setProgressPercentage(total > 0 ? (processed * 100.0) / total : 0);
            Map<String, Object> metadata = job.getResultMetadata() != null
                    ? new LinkedHashMap<>(job.getResultMetadata())
                    : new LinkedHashMap<>();
            metadata.put("currentPhase", stage);
            metadata.put("currentEntityType", entityType);
            @SuppressWarnings("unchecked")
            Map<String, Object> stages = metadata.get("stages") instanceof Map<?, ?> m
                    ? new LinkedHashMap<>((Map<String, Object>) m)
                    : new LinkedHashMap<>();
            stages.put(stage, Map.of("completed", processed, "total", total));
            metadata.put("stages", stages);
            job.setResultMetadata(metadata);
            migrationJobRepository.save(job);
        }

        if (logMessage != null && !logMessage.isBlank()) {
            jobLogService.appendLog(jobId, "INFO", logMessage);
        }

        String uid = userId != null ? userId : "anonymous";
        JobProgressUpdate update = JobProgressUpdate.builder()
                .jobId(jobId.toString())
                .progressPercentage(total > 0 ? (int) ((processed * 100.0) / total) : 0)
                .processedEntities(processed)
                .totalEntities(total)
                .failedEntities(failed)
                .currentStage(stage)
                .currentEntityType(entityType)
                .logMessage(logMessage)
                .timestamp(Instant.now())
                .build();

        webSocketHandler.sendProgressUpdate(jobId.toString(), uid, update);
        webSocketHandler.broadcastProgress(jobId.toString(), update);
        pollingFallbackService.cacheProgress(jobId.toString(), update);
    }
}
