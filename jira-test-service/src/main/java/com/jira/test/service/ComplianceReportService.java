package com.jira.test.service;

import com.jira.test.entity.AuditLog;
import com.jira.test.enums.AuditAction;
import com.jira.test.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceReportService {

    private final AuditLogRepository auditLogRepository;

    public Map<String, Object> generateComplianceReport(UUID projectId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        report.put("projectId", projectId);
        report.put("periodStart", startDate);
        report.put("periodEnd", endDate);
        report.put("generatedAt", LocalDateTime.now());

        // Count actions by type
        Map<String, Long> actionCounts = new HashMap<>();
        for (AuditAction action : AuditAction.values()) {
            long count = auditLogRepository.countByProjectAndActionSince(projectId, action, startDate);
            if (count > 0) {
                actionCounts.put(action.name(), count);
            }
        }
        report.put("actionCounts", actionCounts);

        // Get execution failures
        List<AuditLog> failures = auditLogRepository.findByProjectAndActionBetween(
                projectId, AuditAction.EXECUTION_FAILED, startDate, endDate);
        report.put("executionFailures", failures.size());

        // Get step failures
        List<AuditLog> stepFailures = auditLogRepository.findByProjectAndActionBetween(
                projectId, AuditAction.STEP_FAILED, startDate, endDate);
        report.put("stepFailures", stepFailures.size());

        // Get permission changes
        List<AuditLog> permissionChanges = auditLogRepository.findByProjectAndActions(projectId,
                List.of(AuditAction.PERMISSION_GRANTED, AuditAction.PERMISSION_REVOKED,
                        AuditAction.USER_ADDED_TO_PROJECT, AuditAction.USER_REMOVED_FROM_PROJECT));
        report.put("permissionChanges", permissionChanges.size());

        // Get import activities
        List<AuditLog> imports = auditLogRepository.findByProjectAndActions(projectId,
                List.of(AuditAction.CUCUMBER_IMPORTED, AuditAction.JUNIT_IMPORTED, AuditAction.IMPORT_FAILED));
        report.put("importActivities", imports.size());

        // Get test creation/deletion summary
        long testCreated = auditLogRepository.countByProjectAndActionSince(projectId, AuditAction.TEST_CREATED, startDate);
        long testDeleted = auditLogRepository.countByProjectAndActionSince(projectId, AuditAction.TEST_DELETED, startDate);
        report.put("testsCreated", testCreated);
        report.put("testsDeleted", testDeleted);

        // Get execution summary
        long executionsStarted = auditLogRepository.countByProjectAndActionSince(projectId, AuditAction.EXECUTION_STARTED, startDate);
        long executionsCompleted = auditLogRepository.countByProjectAndActionSince(projectId, AuditAction.EXECUTION_COMPLETED, startDate);
        report.put("executionsStarted", executionsStarted);
        report.put("executionsCompleted", executionsCompleted);

        // Calculate compliance score (example formula)
        long totalActions = actionCounts.values().stream().mapToLong(Long::longValue).sum();
        long criticalActions = stepFailures.size() + failures.size();
        double complianceScore = totalActions > 0 ? ((totalActions - criticalActions) * 100.0 / totalActions) : 100.0;
        report.put("complianceScore", Math.round(complianceScore * 100.0) / 100.0);
        report.put("totalActions", totalActions);

        return report;
    }

    public List<Map<String, Object>> getUserActivityReport(UUID projectId, UUID userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AuditLog> logs = auditLogRepository.findByProjectSince(projectId, since);

        return logs.stream()
                .filter(log -> log.getUserId() != null && log.getUserId().equals(userId))
                .map(log -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("action", log.getAction().name());
                    entry.put("entityType", log.getEntityType());
                    entry.put("entityId", log.getEntityId());
                    entry.put("entityName", log.getEntityName());
                    entry.put("timestamp", log.getActionTimestamp());
                    entry.put("status", log.getStatus());
                    entry.put("ipAddress", log.getIpAddress());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserActivitySummary(UUID projectId, UUID userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AuditLog> logs = auditLogRepository.findByProjectSince(projectId, since);

        List<AuditLog> userLogs = logs.stream()
                .filter(log -> log.getUserId() != null && log.getUserId().equals(userId))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("periodDays", days);
        summary.put("totalActions", userLogs.size());

        // Group by action type
        Map<String, Long> actionBreakdown = userLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getAction().name(),
                        Collectors.counting()));
        summary.put("actionBreakdown", actionBreakdown);

        // Group by entity type
        Map<String, Long> entityBreakdown = userLogs.stream()
                .filter(log -> log.getEntityType() != null)
                .collect(Collectors.groupingBy(
                        log -> log.getEntityType(),
                        Collectors.counting()));
        summary.put("entityBreakdown", entityBreakdown);

        // Success/failure ratio
        long successes = userLogs.stream().filter(log -> "SUCCESS".equals(log.getStatus())).count();
        long failures = userLogs.stream().filter(log -> "FAILURE".equals(log.getStatus())).count();
        summary.put("successCount", successes);
        summary.put("failureCount", failures);

        return summary;
    }

    public String exportComplianceData(UUID projectId, LocalDateTime startDate, LocalDateTime endDate, String format) {
        List<AuditLog> logs = auditLogRepository.findByProjectSince(projectId, startDate);

        StringBuilder sb = new StringBuilder();

        if ("CSV".equalsIgnoreCase(format)) {
            // CSV Header
            sb.append("ID,Action,EntityType,EntityID,EntityName,ProjectID,UserID,UserName,Status,Timestamp,ChangeDescription\n");

            for (AuditLog log : logs) {
                sb.append(escapeCsv(log.getId() != null ? log.getId().toString() : ""))
                        .append(",").append(escapeCsv(log.getAction() != null ? log.getAction().name() : ""))
                        .append(",").append(escapeCsv(log.getEntityType() != null ? log.getEntityType() : ""))
                        .append(",").append(escapeCsv(log.getEntityId() != null ? log.getEntityId().toString() : ""))
                        .append(",").append(escapeCsv(log.getEntityName() != null ? log.getEntityName() : ""))
                        .append(",").append(escapeCsv(log.getProjectId() != null ? log.getProjectId().toString() : ""))
                        .append(",").append(escapeCsv(log.getUserId() != null ? log.getUserId().toString() : ""))
                        .append(",").append(escapeCsv(log.getUserName() != null ? log.getUserName() : ""))
                        .append(",").append(escapeCsv(log.getStatus() != null ? log.getStatus() : ""))
                        .append(",").append(escapeCsv(log.getActionTimestamp() != null ? log.getActionTimestamp().toString() : ""))
                        .append(",").append(escapeCsv(log.getChangeDescription() != null ? log.getChangeDescription() : ""))
                        .append("\n");
            }
        } else {
            // JSON format
            sb.append("[\n");
            boolean first = true;
            for (AuditLog log : logs) {
                if (!first) sb.append(",\n");
                first = false;
                sb.append("{");
                sb.append("\"id\":\"").append(log.getId()).append("\"");
                sb.append(",\"action\":\"").append(log.getAction()).append("\"");
                sb.append(",\"entityType\":\"").append(nullSafe(log.getEntityType())).append("\"");
                sb.append(",\"entityId\":\"").append(nullSafe(log.getEntityId())).append("\"");
                sb.append(",\"entityName\":\"").append(nullSafe(log.getEntityName())).append("\"");
                sb.append(",\"projectId\":\"").append(nullSafe(log.getProjectId())).append("\"");
                sb.append(",\"userId\":\"").append(nullSafe(log.getUserId())).append("\"");
                sb.append(",\"userName\":\"").append(nullSafe(log.getUserName())).append("\"");
                sb.append(",\"status\":\"").append(nullSafe(log.getStatus())).append("\"");
                sb.append(",\"timestamp\":\"").append(log.getActionTimestamp()).append("\"");
                sb.append("}");
            }
            sb.append("\n]");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}