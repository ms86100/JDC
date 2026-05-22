package com.jira.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private String serviceName;
    private String entityType;
    private UUID entityId;
    private String action;
    private Map<String, Object> changes;
    private String ipAddress;
    private LocalDateTime createdAt;
}
