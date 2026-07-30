package com.avionics_systems.audit.service;

import com.avionics_systems.audit.dto.AuditEvent;
import com.avionics_systems.audit.dto.AuditLogResponse;
import com.avionics_systems.audit.entity.AuditLog;
import com.avionics_systems.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLogResponse logEvent(AuditEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .userId(event.getUserId())
                .serviceName(event.getServiceName())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .changes(event.getChanges())
                .ipAddress(event.getIpAddress())
                .createdAt(LocalDateTime.now())
                .build();

        auditLog = auditLogRepository.save(auditLog);
        return toResponse(auditLog);
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

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .serviceName(auditLog.getServiceName())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .changes(auditLog.getChanges())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
