package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PermissionSchemeDto {
    private String id;
    private String name;
    private String description;
    private String scope;
    private Boolean isDefault;
    private List<PermissionGrantDto> permissions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PermissionGrantDto {
        private String permissionKey;
        private String entityType;
        private String entityId;
        private String groupName;
        private String userId;
    }
}