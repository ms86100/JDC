package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateStatusRequest {

    private String name;

    private String description;

    private String statusCategory;

    private String statusColor;

    private String iconUrl;

    private Integer sequence;

    private Boolean isDefault;

    private Boolean isActive;

    private Boolean isArchived;

    private String lookupGroup;
}
