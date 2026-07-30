package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for Audit Log operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditLogResponse {

    @EqualsAndHashCode.Include
    private String id;

    private String entityType;
    private String entityId;
    private String entityName;
    private String action;
    private String userId;
    private String username;
    private String userDisplayName;
    private String ipAddress;
    private String userAgent;
    private String description;
    private Map<String, Object> changedFields;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private String category;
    private String source;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;
}