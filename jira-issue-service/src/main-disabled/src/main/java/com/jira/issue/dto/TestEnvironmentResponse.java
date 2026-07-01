package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for test environment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEnvironmentResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String environmentType;
    private Map<String, Object> config;
    private String url;
    private Map<String, String> variables;
    private Boolean isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}