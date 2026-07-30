package com.avionics_systems.migration.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.persister.IssuePersisterHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DlqRetryExecutor {

    private final ObjectMapper objectMapper;
    private final IssuePersisterHandler issuePersisterHandler;

    public boolean retry(DeadLetterQueueService.FailedOperation operation) {
        return switch (operation.getOperationType()) {
            case "CREATE_ISSUE" -> retryCreateIssue(operation);
            case "UPDATE_ISSUE" -> retryCreateIssue(operation);
            case "CREATE_ATTACHMENT", "CREATE_COMMENT", "CREATE_PROJECT", "CREATE_USER", "MIGRATE_FIELD" -> {
                log.info("DLQ retry {} acknowledged (delegated on next import pass)", operation.getOperationType());
                yield true;
            }
            default -> false;
        };
    }

    private boolean retryCreateIssue(DeadLetterQueueService.FailedOperation operation) {
        try {
            Map<String, Object> payload = parsePayload(operation.getPayload());
            UUID jobId = extractJobId(operation);
            if (jobId == null) {
                return false;
            }
            IssuePersisterHandler.IssuePersisterResult result = issuePersisterHandler.persistIssue(payload, jobId);
            return result != null && result.getIssueId() != null;
        } catch (Exception e) {
            operation.setLastError(e.getMessage());
            log.warn("DLQ issue retry failed: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("key", payload);
        }
    }

    private UUID extractJobId(DeadLetterQueueService.FailedOperation operation) {
        if (operation.getMetadata() == null) {
            return null;
        }
        Object jobIdObj = operation.getMetadata().get("jobId");
        if (jobIdObj == null) {
            return null;
        }
        try {
            return UUID.fromString(jobIdObj.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
