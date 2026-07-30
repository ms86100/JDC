package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MigrationJobLogService {

    @Value("${app.job.max-logs:200}")
    private int maxLogs;

    private final MigrationJobRepository migrationJobRepository;

    @Transactional
    public void appendLog(UUID jobId, String level, String message) {
        MigrationJob job = migrationJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        Map<String, Object> metadata = job.getResultMetadata() != null
                ? new LinkedHashMap<>(job.getResultMetadata())
                : new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> logs = metadata.get("recentLogs") instanceof List<?> list
                ? new ArrayList<>((List<Map<String, Object>>) list)
                : new ArrayList<>();

        logs.add(Map.of(
                "timestamp", Instant.now().toString(),
                "level", level != null ? level : "INFO",
                "message", message != null ? message : ""
        ));
        if (logs.size() > maxLogs) {
            logs = new ArrayList<>(logs.subList(logs.size() - maxLogs, logs.size()));
        }
        metadata.put("recentLogs", logs);
        job.setResultMetadata(metadata);
        migrationJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentLogs(UUID jobId) {
        return migrationJobRepository.findById(jobId)
                .map(j -> {
                    if (j.getResultMetadata() == null) {
                        return List.<Map<String, Object>>of();
                    }
                    Object raw = j.getResultMetadata().get("recentLogs");
                    if (raw instanceof List<?> list) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> typed = (List<Map<String, Object>>) list;
                        return typed;
                    }
                    return List.<Map<String, Object>>of();
                })
                .orElse(List.of());
    }
}
