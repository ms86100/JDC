package com.jira.admin.service;

import com.jira.admin.entity.AuditLogEntity;
import com.jira.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Audit Service - Enterprise audit logging and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogs(
            String userId,
            String category,
            String action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());

        return auditLogRepository.findAll(pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByTimestampBetween(start, end, pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogsByUser(String userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByUserId(userId, pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogsByCategory(String category, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByCategory(category, pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogsByEntity(String entityType, String entityId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageRequest);
    }

    @Transactional
    public void logAudit(String action, String category, String entityType, String entityId, String entityName,
                        Map<String, Object> changedValues, String details, String result, String severity, String source,
                        String userId, String userName, String userIp, String userAgent) {

        String changedValuesJson = null;
        if (changedValues != null) {
            try {
                changedValuesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(changedValues);
            } catch (Exception e) {
                changedValuesJson = changedValues.toString();
            }
        }

        AuditLogEntity auditLog = AuditLogEntity.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .userName(userName)
                .userIp(userIp)
                .action(action)
                .category(category)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .changedValues(changedValuesJson)
                .details(details)
                .result(result)
                .severity(severity != null ? severity : "INFO")
                .source(source != null ? source : "UI")
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAuditStatistics(LocalDateTime start, LocalDateTime end) {
        List<AuditLogEntity> logs = auditLogRepository.findByTimestampAfter(start);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", logs.size());

        // Count by category
        Map<String, Long> byCategory = new HashMap<>();
        for (AuditLogEntity log : logs) {
            String cat = log.getCategory();
            byCategory.put(cat, byCategory.getOrDefault(cat, 0L) + 1);
        }
        stats.put("byCategory", byCategory);

        // Count by action
        Map<String, Long> byAction = new HashMap<>();
        for (AuditLogEntity log : logs) {
            String act = log.getAction();
            byAction.put(act, byAction.getOrDefault(act, 0L) + 1);
        }
        stats.put("byAction", byAction);

        // Count by result
        Map<String, Long> byResult = new HashMap<>();
        for (AuditLogEntity log : logs) {
            String res = log.getResult();
            byResult.put(res, byResult.getOrDefault(res, 0L) + 1);
        }
        stats.put("byResult", byResult);

        // Most active users
        Map<String, Long> byUser = new HashMap<>();
        for (AuditLogEntity log : logs) {
            if (log.getUserId() != null) {
                byUser.put(log.getUserId(), byUser.getOrDefault(log.getUserId(), 0L) + 1);
            }
        }
        stats.put("mostActiveUsers", byUser.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll));

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivity(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("timestamp").descending());
        Page<AuditLogEntity> page = auditLogRepository.findAll(pageRequest);

        return page.getContent().stream().map(log -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("timestamp", log.getTimestamp());
            item.put("userName", log.getUserName());
            item.put("action", log.getAction());
            item.put("category", log.getCategory());
            item.put("entityName", log.getEntityName());
            item.put("result", log.getResult());
            item.put("severity", log.getSeverity());
            return item;
        }).toList();
    }
}