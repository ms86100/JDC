package com.jira.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupDto {
    private UUID id;
    private String groupName;
    private String description;
    private String groupType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Integer memberCount;
}