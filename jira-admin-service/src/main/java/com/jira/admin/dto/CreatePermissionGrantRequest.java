package com.jira.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreatePermissionGrantRequest {

    @NotBlank(message = "Permission ID is required")
    private String permissionId;

    private String permissionKey;

    @NotBlank(message = "Holder type is required")
    private String holderType;  // 'USER', 'GROUP', 'PROJECT_ROLE'

    @NotBlank(message = "Holder ID is required")
    private String holderId;

    private String holderName;
}