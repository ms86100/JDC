package com.jira.migration.persister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Worklog Persister Handler
 * Handles worklog entity creation with time tracking support
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorklogPersisterHandler {

    @Transactional(rollbackFor = Exception.class)
    public WorklogPersistResult persistWorklog(Map<String, Object> worklogData, UUID jobId) {
        WorklogPersistResult result = new WorklogPersistResult();

        try {
            String issueKey = (String) worklogData.get("issueKey");
            if (issueKey == null) {
                throw new IllegalArgumentException("Issue key is required");
            }

            Integer timeSpentSeconds = parseTimeSpent(worklogData);
            if (timeSpentSeconds == null || timeSpentSeconds <= 0) {
                throw new IllegalArgumentException("Time spent is required and must be positive");
            }

            WorklogEntity worklog = WorklogEntity.builder()
                    .issueKey(issueKey)
                    .timeSpentSeconds(timeSpentSeconds)
                    .timeSpentFormatted((String) worklogData.get("timeSpentFormatted"))
                    .startedAt(worklogData.get("startedAt") != null ?
                            java.time.LocalDateTime.parse(worklogData.get("startedAt").toString()) :
                            java.time.LocalDateTime.now())
                    .authorId((UUID) worklogData.get("authorId"))
                    .comment((String) worklogData.get("comment"))
                    .build();

            UUID worklogId = persistToDatabase(worklog);

            result.setSuccess(true);
            result.setWorklogId(worklogId);

            // Update issue time tracking
            updateIssueTimeTracking(issueKey, timeSpentSeconds);

            log.debug("Persisted worklog for issue {}: {} seconds", issueKey, timeSpentSeconds);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private Integer parseTimeSpent(Map<String, Object> worklogData) {
        // Support multiple formats: "3h", "1d", "30m", "3600" (seconds)
        Object timeSpent = worklogData.get("timeSpentSeconds");
        if (timeSpent instanceof Integer) {
            return (Integer) timeSpent;
        }

        String formatted = (String) worklogData.get("timeSpentFormatted");
        if (formatted != null && !formatted.isBlank()) {
            return parseJiraTimeFormat(formatted);
        }

        return null;
    }

    private Integer parseJiraTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;

        int totalSeconds = 0;
        String remaining = timeStr.trim();

        // Parse weeks
        int weeksIdx = remaining.indexOf('w');
        if (weeksIdx > 0) {
            String weeks = remaining.substring(0, weeksIdx).trim();
            totalSeconds += Integer.parseInt(weeks) * 7 * 8 * 60 * 60;
            remaining = remaining.substring(weeksIdx + 1);
        }

        // Parse days
        int daysIdx = remaining.indexOf('d');
        if (daysIdx > 0) {
            String days = remaining.substring(0, daysIdx).trim();
            totalSeconds += Integer.parseInt(days) * 8 * 60 * 60;
            remaining = remaining.substring(daysIdx + 1);
        }

        // Parse hours
        int hoursIdx = remaining.indexOf('h');
        if (hoursIdx > 0) {
            String hours = remaining.substring(0, hoursIdx).trim();
            totalSeconds += Integer.parseInt(hours) * 60 * 60;
            remaining = remaining.substring(hoursIdx + 1);
        }

        // Parse minutes
        int minIdx = remaining.indexOf('m');
        if (minIdx > 0) {
            String mins = remaining.substring(0, minIdx).trim();
            totalSeconds += Integer.parseInt(mins) * 60;
        }

        return totalSeconds > 0 ? totalSeconds : null;
    }

    private void updateIssueTimeTracking(String issueKey, int timeSpentSeconds) {
        log.debug("Updating time tracking for issue {}: +{} seconds", issueKey, timeSpentSeconds);
        // In production: Call issue-service to update aggregate time tracking
    }

    private UUID persistToDatabase(WorklogEntity worklog) {
        log.debug("Persisting worklog for issue {}", worklog.getIssueKey());
        return UUID.randomUUID();
    }

    @lombok.Data
    @lombok.Builder
    public static class WorklogEntity {
        private UUID id;
        private String issueKey;
        private Integer timeSpentSeconds;
        private String timeSpentFormatted;
        private java.time.LocalDateTime startedAt;
        private UUID authorId;
        private String comment;
    }

    public static class WorklogPersistResult {
        private boolean success;
        private UUID worklogId;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getWorklogId() { return worklogId; }
        public void setWorklogId(UUID worklogId) { this.worklogId = worklogId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}