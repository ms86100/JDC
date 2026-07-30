package com.avionics_systems.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request DTO for creating an Audit Log.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditLogRequest {

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotBlank(message = "Entity ID is required")
    private String entityId;

    @NotBlank(message = "Action is required")
    private String action;

    private String userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String description;
    private Map<String, Object> changedFields;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private String category;
    private String source;
    private LocalDateTime timestamp;
}