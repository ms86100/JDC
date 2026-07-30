package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PermissionGrantDto {
    private String id;
    private String permissionSchemeId;
    private String permissionId;
    private String permissionKey;
    private String holderType;
    private String holderId;
    private String holderName;
    private String createdAt;
}