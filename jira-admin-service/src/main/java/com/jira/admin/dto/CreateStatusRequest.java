package com.jira.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateStatusRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String statusCategory;

    private String statusColor;

    private String iconUrl;

    private Integer sequence;

    private Boolean isDefault;

    private String lookupGroup;
}
