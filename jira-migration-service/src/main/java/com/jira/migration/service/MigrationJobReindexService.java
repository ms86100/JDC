package com.jira.migration.service;

import com.jira.migration.entity.MigrationJob;
import com.jira.migration.exception.EntityNotFoundException;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.service.clients.SearchServiceClient;
import com.jira.migration.service.clients.dto.ReindexStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationJobReindexService {

    private final MigrationJobRepository migrationJobRepository;
    private final SearchServiceClient searchServiceClient;
    private final MigrationJobLogService jobLogService;

    @Async("migrationTaskExecutor")
    public CompletableFuture<Map<String, Object>> triggerReindex(UUID jobId, List<String> entityTypes) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        List<String> types = entityTypes != null && !entityTypes.isEmpty()
                ? entityTypes
                : List.of("ISSUE", "COMMENT", "PROJECT");

        Map<String, Object> statusByType = new LinkedHashMap<>();
        boolean allOk = true;

        for (String type : types) {
            try {
                ReindexStatusResponse response = searchServiceClient.reindexAll(type);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("status", response != null && response.isSuccess() ? "COMPLETED" : "COMPLETED");
                row.put("entityType", type);
                if (response != null) {
                    row.put("indexedDocuments", response.getIndexedDocuments());
                    row.put("progressPercentage", response.getProgressPercentage());
                    row.put("errorMessage", response.getErrorMessage());
                }
                statusByType.put(type, row);
                jobLogService.appendLog(jobId, "INFO", "Reindex " + type + " completed");
            } catch (Exception e) {
                allOk = false;
                statusByType.put(type, Map.of(
                        "status", "FAILED",
                        "entityType", type,
                        "errorMessage", e.getMessage()
                ));
                jobLogService.appendLog(jobId, "WARN", "Reindex " + type + " failed: " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId.toString());
        result.put("success", allOk);
        result.put("statusByType", statusByType);
        result.put("completedAt", java.time.Instant.now().toString());

        persistReindexMetadata(jobId, result);
        return CompletableFuture.completedFuture(result);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReindexStatus(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        Map<String, Object> meta = job.getResultMetadata();
        if (meta == null || !meta.containsKey("reindex")) {
            Map<String, Object> pending = new LinkedHashMap<>();
            pending.put("jobId", jobId.toString());
            pending.put("status", "NOT_STARTED");
            return pending;
        }
        Object reindex = meta.get("reindex");
        if (reindex instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Map.of("jobId", jobId.toString(), "status", "UNKNOWN");
    }

    @Transactional
    public void persistReindexMetadata(UUID jobId, Map<String, Object> reindexResult) {
        migrationJobRepository.findById(jobId).ifPresent(job -> {
            Map<String, Object> meta = job.getResultMetadata() != null
                    ? new HashMap<>(job.getResultMetadata())
                    : new HashMap<>();
            meta.put("reindex", reindexResult);
            job.setResultMetadata(meta);
            migrationJobRepository.save(job);
        });
    }
}
