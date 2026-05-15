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
public class CreateBoardPermissionRequest {
    private String permissionType;  // VIEW, EDIT, ADMIN, MANAGE_SPRINTS, EDIT_SPRINTS
    private String principalType = "USER";  // USER, GROUP
    private UUID principalId;
}