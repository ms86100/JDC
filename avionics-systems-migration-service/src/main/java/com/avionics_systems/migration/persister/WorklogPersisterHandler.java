package com.avionics_systems.migration.persister;

import com.avionics_systems.migration.service.clients.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Worklog Persister Handler — persists to issue-service worklog API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorklogPersisterHandler {

    private final IssueServiceClient issueServiceClient;

    @Transactional(rollbackFor = Exception.class)
    public WorklogPersistResult persistWorklog(Map<String, Object> worklogData, UUID jobId) {
        WorklogPersistResult result = new WorklogPersistResult();

        try {
            String issueId = (String) worklogData.get("issueId");
            String issueKey = (String) worklogData.get("issueKey");
            if (issueId == null && issueKey != null) {
                issueServiceClient.getIssueByKey(issueKey).ifPresent(i -> worklogData.put("issueId", i.getId()));
                issueId = (String) worklogData.get("issueId");
            }
            if (issueId == null) {
                throw new IllegalArgumentException("Issue id or key is required for worklog");
            }

            Integer timeSpentSeconds = parseTimeSpent(worklogData);
            if (timeSpentSeconds == null || timeSpentSeconds <= 0) {
                throw new IllegalArgumentException("Time spent is required and must be positive");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("issueId", issueId);
            payload.put("timeSpentSeconds", timeSpentSeconds.longValue());
            payload.put("workDescription", worklogData.get("comment"));
            if (worklogData.get("startedAt") != null) {
                payload.put("startedAt", worklogData.get("startedAt").toString());
            }

            Map<String, Object> response = issueServiceClient.createWorklog(issueId, payload);
            Object id = response.get("id");
            UUID worklogId = id != null ? UUID.fromString(id.toString()) : UUID.randomUUID();

            result.setSuccess(true);
            result.setWorklogId(worklogId);
            log.debug("Persisted worklog for issue {}: {} seconds", issueKey != null ? issueKey : issueId, timeSpentSeconds);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private Integer parseTimeSpent(Map<String, Object> worklogData) {
        Object timeSpent = worklogData.get("timeSpentSeconds");
        if (timeSpent instanceof Integer) {
            return (Integer) timeSpent;
        }
        if (timeSpent instanceof Number) {
            return ((Number) timeSpent).intValue();
        }

        String formatted = (String) worklogData.get("timeSpentFormatted");
        if (formatted != null && !formatted.isBlank()) {
            return parseLegacyTimeFormat(formatted);
        }
        return null;
    }

    private Integer parseLegacyTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        int totalSeconds = 0;
        String remaining = timeStr.trim();

        int weeksIdx = remaining.indexOf('w');
        if (weeksIdx > 0) {
            totalSeconds += Integer.parseInt(remaining.substring(0, weeksIdx).trim()) * 7 * 8 * 60 * 60;
            remaining = remaining.substring(weeksIdx + 1);
        }
        int daysIdx = remaining.indexOf('d');
        if (daysIdx > 0) {
            totalSeconds += Integer.parseInt(remaining.substring(0, daysIdx).trim()) * 8 * 60 * 60;
            remaining = remaining.substring(daysIdx + 1);
        }
        int hoursIdx = remaining.indexOf('h');
        if (hoursIdx > 0) {
            totalSeconds += Integer.parseInt(remaining.substring(0, hoursIdx).trim()) * 60 * 60;
            remaining = remaining.substring(hoursIdx + 1);
        }
        int minIdx = remaining.indexOf('m');
        if (minIdx > 0) {
            totalSeconds += Integer.parseInt(remaining.substring(0, minIdx).trim()) * 60;
        }
        return totalSeconds > 0 ? totalSeconds : null;
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
