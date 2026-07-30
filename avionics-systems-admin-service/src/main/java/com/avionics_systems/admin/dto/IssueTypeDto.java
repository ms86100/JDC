package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueTypeDto {
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private String issueTypeKey;
    private Integer typeOrder;
    private Boolean isSubtask;
    private Boolean isArchived;
}