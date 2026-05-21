package com.jira.migration.service;

import com.jira.migration.entity.MigrationRetryQueueEntry;
import com.jira.migration.persister.IssuePersisterHandler;
import com.jira.migration.repository.MigrationRetryQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationRetryQueueProcessor {

    private final MigrationRetryQueueRepository retryQueueRepository;
    private final IssuePersisterHandler issuePersisterHandler;

    @Transactional
    public MigrationRetryQueueEntry enqueue(
            java.util.UUID jobId,
            String entityType,
            String entityKey,
            String operation,
            Map<String, Object> payload) {
        return retryQueueRepository.save(MigrationRetryQueueEntry.builder()
                .jobId(jobId)
                .entityType(entityType)
                .entityKey(entityKey)
                .operation(operation)
                .payload(payload)
                .status("PENDING")
                .nextRetryAt(LocalDateTime.now().plusSeconds(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Scheduled(fixedDelayString = "${migration.retry.process-interval-ms:20000}")
    @Transactional
    public void processDue() {
        for (MigrationRetryQueueEntry entry : retryQueueRepository.findDue(LocalDateTime.now())) {
            entry.setAttempts(entry.getAttempts() + 1);
            try {
                boolean ok = retryEntry(entry);
                if (ok) {
                    entry.setStatus("COMPLETED");
                } else if (entry.getAttempts() >= entry.getMaxAttempts()) {
                    entry.setStatus("FAILED");
                    entry.setLastError("Max attempts exceeded");
                } else {
                    entry.setNextRetryAt(LocalDateTime.now().plusMinutes(entry.getAttempts() * 2L));
                }
            } catch (Exception e) {
                entry.setLastError(e.getMessage());
                if (entry.getAttempts() >= entry.getMaxAttempts()) {
                    entry.setStatus("FAILED");
                } else {
                    entry.setNextRetryAt(LocalDateTime.now().plusMinutes(entry.getAttempts() * 2L));
                }
            }
            entry.setUpdatedAt(LocalDateTime.now());
            retryQueueRepository.save(entry);
        }
    }

    private boolean retryEntry(MigrationRetryQueueEntry entry) {
        if ("CREATE_ISSUE".equals(entry.getOperation()) && entry.getPayload() != null) {
            IssuePersisterHandler.IssuePersisterResult r =
                    issuePersisterHandler.persistIssue(entry.getPayload(), entry.getJobId());
            return r != null && r.isSuccess();
        }
        log.debug("Retry queue op {} acknowledged", entry.getOperation());
        return true;
    }
}
