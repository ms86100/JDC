package com.jira.test.service;

import com.jira.test.entity.AuditLog;
import com.jira.test.enums.AuditAction;
import com.jira.test.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // SSE emitter registry for real-time streaming
    private final Map<UUID, List<SseEmitter>> projectEmitters = new ConcurrentHashMap<>();
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    // Compliance report templates
    private static final Map<String, ComplianceTemplate> COMPLIANCE_TEMPLATES = new HashMap<>();
    static {
        COMPLIANCE_TEMPLATES.put("SOX", new ComplianceTemplate("SOX", "Sarbanes-Oxley Act compliance report", List.of("TEST_CREATED", "TEST_UPDATED", "TEST_DELETED", "PERMISSION_GRANTED", "PERMISSION_REVOKED", "USER_ADDED_TO_PROJECT", "USER_REMOVED_FROM_PROJECT")));
        COMPLIANCE_TEMPLATES.put("GDPR", new ComplianceTemplate("GDPR", "General Data Protection Regulation compliance", List.of("TEST_DELETED", "USER_REMOVED_FROM_PROJECT", "EXPORT_PERFORMED")));
        COMPLIANCE_TEMPLATES.put("HIPAA", new ComplianceTemplate("HIPAA", "Health Insurance Portability and Accountability Act", List.of("TEST_CREATED", "TEST_UPDATED", "TEST_DELETED", "EXECUTION_STARTED", "EXECUTION_COMPLETED")));
    }

    // Anomaly detection thresholds
    private static final int ANOMALY_BURST_THRESHOLD = 20; // Actions per minute
    private static final int ANOMALY_FAILED_LOGIN_THRESHOLD = 5; // Failed logins
    private static final int ANOMALY_UNUSUAL_HOUR_START = 23; // 11 PM
    private static final int ANOMALY_UNUSUAL_HOUR_END = 5; // 5 AM

    @Async
    public void logAction(AuditAction action, String entityType, UUID entityId, String entityName,
                          UUID projectId, UUID userId, String userName, UUID... relatedId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);

            // Stream to connected clients
            streamAuditEvent(projectId, auditLog);

            // Check for anomalies
            checkAndLogAnomalies(auditLog);

            log.debug("Audit log recorded: {} for {} {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to log audit action: {}", action, e);
        }
    }

    // ==================== Real-time Streaming ====================

    public SseEmitter subscribeToAuditStream(UUID projectId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        projectEmitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(projectId, emitter));
        emitter.onTimeout(() -> removeEmitter(projectId, emitter));
        emitter.onError(e -> removeEmitter(projectId, emitter));

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("status", "connected", "projectId", projectId.toString())));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event", e);
        }

        log.info("New SSE subscriber for project: {}", projectId);
        return emitter;
    }

    private void streamAuditEvent(UUID projectId, AuditLog auditLog) {
        List<SseEmitter> emitters = projectEmitters.get(projectId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        Map<String, Object> eventData = Map.of(
                "id", auditLog.getId(),
                "action", auditLog.getAction().name(),
                "entityType", auditLog.getEntityType() != null ? auditLog.getEntityType() : "",
                "entityId", auditLog.getEntityId() != null ? auditLog.getEntityId().toString() : "",
                "entityName", auditLog.getEntityName() != null ? auditLog.getEntityName() : "",
                "userName", auditLog.getUserName() != null ? auditLog.getUserName() : "",
                "status", auditLog.getStatus() != null ? auditLog.getStatus() : "",
                "timestamp", auditLog.getActionTimestamp() != null ? auditLog.getActionTimestamp().toString() : ""
        );

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("AUDIT_EVENT")
                        .data(eventData));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        deadEmitters.forEach(emitters::remove);
    }

    private void removeEmitter(UUID projectId, SseEmitter emitter) {
        List<SseEmitter> emitters = projectEmitters.get(projectId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    // ==================== Compliance Reports ====================

    public ComplianceReport generateComplianceReport(UUID projectId, String templateType,
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        ComplianceTemplate template = COMPLIANCE_TEMPLATES.get(templateType);
        if (template == null) {
            throw new IllegalArgumentException("Unknown compliance template: " + templateType);
        }

        List<AuditLog> relevantLogs = auditLogRepository.findByProjectAndActionBetween(
                projectId, null, startDate, endDate);

        // Filter by template's relevant actions
        Set<AuditAction> relevantActions = template.getRelevantActions().stream()
                .map(AuditAction::valueOf)
                .collect(Collectors.toSet());

        List<AuditLog> filteredLogs = relevantLogs.stream()
                .filter(log -> relevantActions.contains(log.getAction()))
                .collect(Collectors.toList());

        // Calculate statistics
        Map<String, Long> actionCounts = filteredLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getAction().name(), Collectors.counting()));

        Map<String, Long> userActivity = filteredLogs.stream()
                .filter(log -> log.getUserName() != null)
                .collect(Collectors.groupingBy(AuditLog::getUserName, Collectors.counting()));

        List<ActivitySummary> activityByDay = calculateActivityByDay(filteredLogs, startDate, endDate);

        // Generate findings
        List<ComplianceFinding> findings = generateComplianceFindings(filteredLogs, template);

        return ComplianceReport.builder()
                .projectId(projectId)
                .templateType(templateType)
                .reportId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalEvents(filteredLogs.size())
                .eventsByAction(actionCounts)
                .eventsByUser(userActivity)
                .activityByDay(activityByDay)
                .findings(findings)
                .compliant(findings.stream().noneMatch(f -> f.getSeverity() == "HIGH" || f.getSeverity() == "CRITICAL"))
                .build();
    }

    private List<ActivitySummary> calculateActivityByDay(List<AuditLog> logs, LocalDateTime start, LocalDateTime end) {
        Map<String, ActivitySummary> summaryMap = new HashMap<>();
        LocalDateTime current = start.toLocalDate().atStartOfDay();

        while (!current.isAfter(end)) {
            String dateKey = current.toLocalDate().toString();
            summaryMap.put(dateKey, new ActivitySummary(dateKey, 0, new HashMap<>()));
            current = current.plusDays(1);
        }

        for (AuditLog log : logs) {
            if (log.getActionTimestamp() != null) {
                String dateKey = log.getActionTimestamp().toLocalDate().toString();
                ActivitySummary summary = summaryMap.get(dateKey);
                if (summary != null) {
                    summary.setTotalEvents(summary.getTotalEvents() + 1);
                    String actionKey = log.getAction().name();
                    summary.getActionBreakdown().merge(actionKey, 1L, Long::sum);
                }
            }
        }

        return new ArrayList<>(summaryMap.values());
    }

    private List<ComplianceFinding> generateComplianceFindings(List<AuditLog> logs, ComplianceTemplate template) {
        List<ComplianceFinding> findings = new ArrayList<>();

        // Check for bulk deletions
        long deletionCount = logs.stream()
                .filter(log -> log.getAction() == AuditAction.TEST_DELETED)
                .count();
        if (deletionCount > 10) {
            findings.add(ComplianceFinding.builder()
                    .type("BULK_DELETION")
                    .severity("MEDIUM")
                    .description("High number of deletions detected: " + deletionCount)
                    .recommendation("Review deletion logs for compliance")
                    .build());
        }

        // Check for permission changes
        long permissionChanges = logs.stream()
                .filter(log -> log.getAction() == AuditAction.PERMISSION_GRANTED ||
                        log.getAction() == AuditAction.PERMISSION_REVOKED)
                .count();
        if (permissionChanges > 0) {
            findings.add(ComplianceFinding.builder()
                    .type("PERMISSION_CHANGE")
                    .severity("LOW")
                    .description("Permission changes detected: " + permissionChanges)
                    .recommendation("Ensure all permission changes are authorized")
                    .build());
        }

        return findings;
    }

    public Map<String, Object> exportComplianceReport(ComplianceReport report, String format) {
        Map<String, Object> result = new HashMap<>();

        switch (format.toUpperCase()) {
            case "PDF":
                result.put("contentType", "application/pdf");
                result.put("data", generatePdfContent(report));
                break;
            case "CSV":
                result.put("contentType", "text/csv");
                result.put("data", generateCsvContent(report));
                break;
            case "JSON":
            default:
                result.put("contentType", "application/json");
                try {
                    result.put("data", objectMapper.writeValueAsString(report));
                } catch (Exception e) {
                    result.put("data", report.toString());
                }
                break;
        }

        return result;
    }

    private byte[] generatePdfContent(ComplianceReport report) {
        // Simplified PDF generation - in production use iText or Apache PDFBox
        StringBuilder sb = new StringBuilder();
        sb.append("COMPLIANCE REPORT\n");
        sb.append("=================\n\n");
        sb.append("Report ID: ").append(report.getReportId()).append("\n");
        sb.append("Template: ").append(report.getTemplateType()).append("\n");
        sb.append("Period: ").append(report.getPeriodStart()).append(" to ").append(report.getPeriodEnd()).append("\n");
        sb.append("Total Events: ").append(report.getTotalEvents()).append("\n");
        sb.append("Status: ").append(report.isCompliant() ? "COMPLIANT" : "NON-COMPLIANT").append("\n\n");

        sb.append("Findings:\n");
        for (ComplianceFinding finding : report.getFindings()) {
            sb.append("- [").append(finding.getSeverity()).append("] ").append(finding.getDescription()).append("\n");
        }

        return sb.toString().getBytes();
    }

    private String generateCsvContent(ComplianceReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Action,Count\n");

        for (ActivitySummary summary : report.getActivityByDay()) {
            for (Map.Entry<String, Long> entry : summary.getActionBreakdown().entrySet()) {
                sb.append(summary.getDate()).append(",")
                        .append(entry.getKey()).append(",")
                        .append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    // ==================== Anomaly Detection ====================

    private final Map<UUID, List<AnomalyDataPoint>> recentActivityCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE = 1000;

    private void checkAndLogAnomalies(AuditLog auditLog) {
        UUID userId = auditLog.getUserId();
        if (userId == null) return;

        LocalDateTime now = LocalDateTime.now();
        List<AnomalyDataPoint> userActivity = recentActivityCache.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // Add current action
        userActivity.add(new AnomalyDataPoint(now, auditLog.getAction().name()));

        // Trim cache
        while (userActivity.size() > CACHE_SIZE) {
            userActivity.remove(0);
        }

        // Check various anomaly types
        checkBurstActivity(userId, userActivity, now);
        checkUnusualHours(userId, now);
        checkFailedActions(auditLog);
        checkSuspiciousPatterns(userId, userActivity);
    }

    private void checkBurstActivity(UUID userId, List<AnomalyDataPoint> activity, LocalDateTime now) {
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);
        long recentCount = activity.stream()
                .filter(dp -> dp.timestamp.isAfter(oneMinuteAgo))
                .count();

        if (recentCount > ANOMALY_BURST_THRESHOLD) {
            logAnomaly("BURST_ACTIVITY", userId,
                    "Unusual burst of activity: " + recentCount + " actions in the last minute",
                    "HIGH");
        }
    }

    private void checkUnusualHours(UUID userId, LocalDateTime now) {
        int hour = now.getHour();
        if (hour >= ANOMALY_UNUSUAL_HOUR_START || hour <= ANOMALY_UNUSUAL_HOUR_END) {
            logAnomaly("UNUSUAL_HOURS", userId,
                    "Activity detected outside normal business hours at " + hour + ":00",
                    "MEDIUM");
        }
    }

    private void checkFailedActions(AuditLog auditLog) {
        if ("FAILURE".equals(auditLog.getStatus())) {
            logAnomaly("ACTION_FAILURE", auditLog.getUserId(),
                    "Failed action: " + auditLog.getAction().name() + " - " + auditLog.getErrorMessage(),
                    "LOW");
        }
    }

    private void checkSuspiciousPatterns(UUID userId, List<AnomalyDataPoint> activity) {
        // Check for rapid action cycling
        if (activity.size() >= 10) {
            List<AnomalyDataPoint> recent = activity.subList(Math.max(0, activity.size() - 10), activity.size());
            Set<String> uniqueActions = recent.stream()
                    .map(dp -> dp.action)
                    .collect(Collectors.toSet());

            // If user is performing many different types of actions rapidly
            if (uniqueActions.size() > 7 && recent.get(recent.size() - 1).timestamp.isAfter(
                    recent.get(0).timestamp.plusSeconds(30))) {
                logAnomaly("RAPID_ACTION_CYCLING", userId,
                        "User performing rapid cycling through different action types",
                        "MEDIUM");
            }
        }
    }

    private void logAnomaly(String type, UUID userId, String description, String severity) {
        log.warn("ANOMALY_DETECTED: type={}, userId={}, description={}, severity={}",
                type, userId, description, severity);

        // Store anomaly in database for reporting
        AuditLog anomalyLog = AuditLog.builder()
                .action(AuditAction.EXECUTION_FAILED) // Using existing action type
                .entityType("ANOMALY")
                .entityId(userId)
                .entityName(type)
                .userId(userId)
                .status("ANOMALY_" + severity)
                .errorMessage(description)
                .build();
        auditLogRepository.save(anomalyLog);
    }

    public List<Map<String, Object>> getDetectedAnomalies(UUID projectId, LocalDateTime since) {
        List<AuditLog> anomalyLogs = auditLogRepository.findByProjectSince(projectId, since);

        return anomalyLogs.stream()
                .filter(log -> "ANOMALY_HIGH".equals(log.getStatus()) ||
                        "ANOMALY_MEDIUM".equals(log.getStatus()) ||
                        "ANOMALY_LOW".equals(log.getStatus()))
                .map(log -> {
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("id", log.getId());
                    anomaly.put("type", log.getEntityName());
                    anomaly.put("userId", log.getUserId());
                    anomaly.put("description", log.getErrorMessage());
                    anomaly.put("severity", log.getStatus().replace("ANOMALY_", ""));
                    anomaly.put("timestamp", log.getActionTimestamp());
                    return anomaly;
                })
                .collect(Collectors.toList());
    }

    // ==================== User Activity Heatmap ====================

    public Map<String, Map<Integer, Long>> generateActivityHeatmap(UUID projectId, LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = auditLogRepository.findByProjectSince(projectId, startDate);

        // Map: userId -> (hour of day -> count)
        Map<String, Map<Integer, Long>> heatmap = new HashMap<>();

        for (AuditLog log : logs) {
            String userName = log.getUserName();
            if (userName == null) continue;

            heatmap.computeIfAbsent(userName, k -> new HashMap<>());

            if (log.getActionTimestamp() != null) {
                int hour = log.getActionTimestamp().getHour();
                heatmap.get(userName).merge(hour, 1L, Long::sum);
            }
        }

        return heatmap;
    }

    // ==================== Audit Data Archival ====================

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void processArchivalPolicies() {
        log.info("Starting audit data archival process");

        LocalDateTime retentionCutoff = LocalDateTime.now().minusYears(7); // 7 year default retention

        // In production, move old records to cold storage/archive table
        List<AuditLog> oldLogs = auditLogRepository.findByProjectSince(null, retentionCutoff);
        log.info("Found {} audit logs older than retention period", oldLogs.size());

        // Mark for archival instead of immediate deletion
        for (AuditLog log : oldLogs) {
            log.setStatus("ARCHIVED");
            auditLogRepository.save(log);
        }

        log.info("Audit archival process completed");
    }

    public void archiveAuditData(UUID projectId, LocalDateTime beforeDate) {
        List<AuditLog> logs = auditLogRepository.findByProjectSince(projectId, beforeDate.minusYears(1));

        for (AuditLog log : logs) {
            if (log.getActionTimestamp().isBefore(beforeDate)) {
                log.setStatus("ARCHIVED");
                auditLogRepository.save(log);
            }
        }

        log.info("Archived audit data for project {} before {}", projectId, beforeDate);
    }

    public long purgeArchivedData(LocalDateTime beforeDate) {
        // Only purge archived data
        LocalDateTime cutoff = beforeDate.minusMonths(6); // 6 months after archival
        log.info("Purging archived audit data before {}", cutoff);
        return 0; // Return count in production
    }

    // ==================== Basic Logging Methods ====================

    public void logTestCreated(UUID testId, String testName, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.TEST_CREATED, "TEST", testId, testName, projectId, userId, userName);
    }

    public void logTestUpdated(UUID testId, String testName, UUID projectId, UUID userId, String userName, String oldValue, String newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.TEST_UPDATED)
                    .entityType("TEST")
                    .entityId(testId)
                    .entityName(testName)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log test update: {}", testId, e);
        }
    }

    public void logTestDeleted(UUID testId, String testName, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.TEST_DELETED, "TEST", testId, testName, projectId, userId, userName);
    }

    public void logTestArchived(UUID testId, String testName, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.TEST_ARCHIVED, "TEST", testId, testName, projectId, userId, userName);
    }

    public void logExecutionStarted(UUID executionId, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.EXECUTION_STARTED, "TEST_EXECUTION", executionId, null, projectId, userId, userName);
    }

    public void logExecutionCompleted(UUID executionId, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.EXECUTION_COMPLETED, "TEST_EXECUTION", executionId, null, projectId, userId, userName);
    }

    public void logExecutionFailed(UUID executionId, UUID projectId, UUID userId, String userName, String errorMessage) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.EXECUTION_FAILED)
                    .entityType("TEST_EXECUTION")
                    .entityId(executionId)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .errorMessage(errorMessage)
                    .status("FAILURE")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
            checkAndLogAnomalies(auditLog);
        } catch (Exception e) {
            log.error("Failed to log execution failure: {}", executionId, e);
        }
    }

    public void logStepPassed(UUID stepId, UUID executionId, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.STEP_PASSED, "STEP_RESULT", stepId, null, projectId, userId, userName);
    }

    public void logStepFailed(UUID stepId, UUID executionId, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.STEP_FAILED, "STEP_RESULT", stepId, null, projectId, userId, userName);
    }

    public void logStepBlocked(UUID stepId, UUID executionId, UUID projectId, UUID userId, String userName) {
        logAction(AuditAction.STEP_BLOCKED, "STEP_RESULT", stepId, null, projectId, userId, userName);
    }

    public void logRequirementLinked(UUID testId, String requirementKey, UUID projectId, UUID userId, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.REQUIREMENT_LINKED)
                    .entityType("TEST")
                    .entityId(testId)
                    .entityName(requirementKey)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log requirement link: {}", testId, e);
        }
    }

    public void logRequirementUnlinked(UUID testId, String requirementKey, UUID projectId, UUID userId, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.REQUIREMENT_UNLINKED)
                    .entityType("TEST")
                    .entityId(testId)
                    .entityName(requirementKey)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log requirement unlink: {}", testId, e);
        }
    }

    public void logDefectLinked(UUID executionId, String defectKey, UUID projectId, UUID userId, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.DEFECT_LINKED)
                    .entityType("TEST_EXECUTION")
                    .entityId(executionId)
                    .entityName(defectKey)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log defect link: {}", executionId, e);
        }
    }

    public void logCucumberImported(int count, UUID projectId, UUID userId, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.CUCUMBER_IMPORTED)
                    .entityType("IMPORT")
                    .entityName(count + " scenarios imported")
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log cucumber import", e);
        }
    }

    public void logJunitImported(int count, UUID projectId, UUID userId, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.JUNIT_IMPORTED)
                    .entityType("IMPORT")
                    .entityName(count + " tests imported")
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log JUnit import", e);
        }
    }

    public void logPermissionGranted(UUID userId, UUID projectId, UUID grantedBy, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.PERMISSION_GRANTED)
                    .entityType("USER")
                    .entityId(userId)
                    .projectId(projectId)
                    .userId(grantedBy)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log permission grant", e);
        }
    }

    public void logPermissionRevoked(UUID userId, UUID projectId, UUID revokedBy, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.PERMISSION_REVOKED)
                    .entityType("USER")
                    .entityId(userId)
                    .projectId(projectId)
                    .userId(revokedBy)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log permission revoke", e);
        }
    }

    public void logUserAddedToProject(UUID userId, UUID projectId, UUID addedBy, String userName) {
        logAction(AuditAction.USER_ADDED_TO_PROJECT, "USER", userId, userName, projectId, addedBy, null);
    }

    public void logUserRemovedFromProject(UUID userId, UUID projectId, UUID removedBy, String userName) {
        logAction(AuditAction.USER_REMOVED_FROM_PROJECT, "USER", userId, userName, projectId, removedBy, null);
    }

    public void logConfigurationChanged(UUID projectId, UUID userId, String userName, String oldValue, String newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.CONFIGURATION_CHANGED)
                    .entityType("PROJECT")
                    .entityId(projectId)
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log configuration change", e);
        }
    }

    public void logExportPerformed(UUID projectId, UUID userId, String userName, String format) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(AuditAction.EXPORT_PERFORMED)
                    .entityType("EXPORT")
                    .entityName(format + " export")
                    .projectId(projectId)
                    .userId(userId)
                    .userName(userName)
                    .status("SUCCESS")
                    .build();
            auditLogRepository.save(auditLog);
            streamAuditEvent(projectId, auditLog);
        } catch (Exception e) {
            log.error("Failed to log export", e);
        }
    }

    // ==================== Query Methods ====================

    public Page<AuditLog> getAuditLogs(UUID projectId, Pageable pageable) {
        return auditLogRepository.findByProjectIdOrderByActionTimestampDesc(projectId, pageable);
    }

    public Page<AuditLog> getAuditLogsByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByActionTimestampDesc(entityType, entityId, pageable);
    }

    public Page<AuditLog> getAuditLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByActionTimestampDesc(userId, pageable);
    }

    public List<AuditLog> getEntityHistory(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<AuditLog> getRecentActivity(UUID projectId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return auditLogRepository.findByProjectSince(projectId, since);
    }

    public long countActions(UUID projectId, AuditAction action, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return auditLogRepository.countByProjectAndActionSince(projectId, action, since);
    }

    public Map<String, Object> getAuditStats(UUID projectId, int days) {
        Map<String, Object> stats = new java.util.HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long totalActions = auditLogRepository.findByProjectSince(projectId, since).size();
        stats.put("totalActions", totalActions);
        stats.put("periodDays", days);
        stats.put("since", since);

        // Count by action type
        Map<String, Long> actionCounts = new java.util.HashMap<>();
        for (AuditAction action : AuditAction.values()) {
            long count = auditLogRepository.countByProjectAndActionSince(projectId, action, since);
            if (count > 0) {
                actionCounts.put(action.name(), count);
            }
        }
        stats.put("actionCounts", actionCounts);

        return stats;
    }

    public List<AuditLog> searchAuditLogs(UUID projectId, AuditAction action, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (action != null && startDate != null && endDate != null) {
            return auditLogRepository.findByProjectAndActionBetween(projectId, action, startDate, endDate);
        } else if (action != null) {
            return auditLogRepository.findByProjectAndActions(projectId, List.of(action));
        } else {
            return auditLogRepository.findByProjectSince(projectId, startDate != null ? startDate : LocalDateTime.now().minusDays(30));
        }
    }

    // ==================== Inner Classes for Compliance ====================

    @lombok.Data
    @lombok.Builder
    public static class ComplianceTemplate {
        private String type;
        private String description;
        private List<String> relevantActions;
    }

    @lombok.Data
    @lombok.Builder
    public static class ComplianceReport {
        private UUID projectId;
        private String templateType;
        private String reportId;
        private LocalDateTime generatedAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private long totalEvents;
        private Map<String, Long> eventsByAction;
        private Map<String, Long> eventsByUser;
        private List<ActivitySummary> activityByDay;
        private List<ComplianceFinding> findings;
        private boolean compliant;
    }

    @lombok.Data
    public static class ActivitySummary {
        private String date;
        private long totalEvents;
        private Map<String, Long> actionBreakdown;

        public ActivitySummary(String date, long totalEvents, Map<String, Long> actionBreakdown) {
            this.date = date;
            this.totalEvents = totalEvents;
            this.actionBreakdown = actionBreakdown;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class ComplianceFinding {
        private String type;
        private String severity;
        private String description;
        private String recommendation;
    }

    @lombok.Data
    public static class AnomalyDataPoint {
        private LocalDateTime timestamp;
        private String action;

        public AnomalyDataPoint(LocalDateTime timestamp, String action) {
            this.timestamp = timestamp;
            this.action = action;
        }
    }
}
