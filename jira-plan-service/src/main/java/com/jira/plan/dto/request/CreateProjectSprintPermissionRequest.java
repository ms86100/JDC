package com.jira.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectSprintPermissionRequest {
    private String permissionKey;  // MANAGE_SPRINTS, START_STOP_SPRINTS, EDIT_SPRINT_NAME_AND_GOAL
    private String principalType = "USER";  // USER, GROUP
    private UUID principalId;
}