package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriorityDto {
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private String color;
    private Integer priorityValue;
    private Boolean isActive;
}