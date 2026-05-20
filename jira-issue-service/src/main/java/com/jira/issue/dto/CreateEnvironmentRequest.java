package com.jira.issue.dto;

import lombok.*;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a test environment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnvironmentRequest {

    private String name;
    private String description;
    private String environmentType; // DEV, STAGING, PROD, CUSTOM
    private Map<String, Object> config;
    private String url;
    private Map<String, String> variables;
    private Map<String, String> credentials;
    private Integer sortOrder;
}