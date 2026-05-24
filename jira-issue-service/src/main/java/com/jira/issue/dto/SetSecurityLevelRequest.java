package com.jira.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for setting issue security level.
 * Used by PUT /api/issues/{issueKey}/security-level endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetSecurityLevelRequest {

    @NotNull(message = "Security level ID is required")
    private UUID securityLevelId;
}