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
    private List<String> permissions;  // List of permission keys
}