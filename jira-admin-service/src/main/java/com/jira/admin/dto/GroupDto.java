package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupDto {
    private String id;
    private String groupName;
    private String description;
    private String groupType;
    private Boolean isActive;
    private Integer memberCount;
    private LocalDateTime createdAt;
}