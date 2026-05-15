package com.jira.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.audit.dto.AuditEvent;
import com.jira.audit.dto.AuditLogResponse;
import com.jira.audit.entity.AuditLog;
import com.jira.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AuditLogResponse logEvent(AuditEvent event) {
        AuditLog log = AuditLog.builder()
                .userId(event.getUserId())
                .serviceName(event.getServiceName())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .changes(toJson(event.getChanges()))
                .ipAddress(event.getIpAddress())
                .createdAt(LocalDateTime.now())
                .build();

        log = auditLogRepository.save(log);
        return toResponse(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchLogs(String serviceName, String entityType, UUID entityId,
                                            UUID userId, String action, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<AuditLog> logs;

        if (entityType != null && entityId != null) {
            logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        } else if (userId != null) {
            logs = auditLogRepository.findByUserId(userId, pageable);
        } else if (serviceName != null) {
            logs = auditLogRepository.findByServiceName(serviceName, pageable);
        } else {
            logs = auditLogRepository.findAll(pageable);
        }

        return logs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsForEntity(String entityType, UUID entityId, int page, int size) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsForUser(UUID userId, int page, int size) {
        return auditLogRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .serviceName(log.getServiceName())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .changes(log.getChanges())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
