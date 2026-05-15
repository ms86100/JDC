package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSprintPermissionResponse {
    private UUID id;
    private UUID projectId;
    private String permissionKey;
    private String principalType;
    private UUID principalId;
    private LocalDateTime createdAt;
    private UUID createdBy;
}