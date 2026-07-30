package com.avionics_systems.issue.dto;

import lombok.*;

import java.util.UUID;

/**
 * Response DTO for security level on an issue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLevelResponse {

    private UUID issueId;
    private String issueKey;
    private UUID securityLevelId;
    private String securityLevelName;
    private String securityLevelDescription;
    private String securityLevelType;
}