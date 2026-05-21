package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderTemplateResponse {

    private UUID id;

    private String name;

    private String description;

    private String category;

    private String folderType;

    private String icon;

    private String color;

    private List<String> tags;

    private Boolean isSystemTemplate;

    private Integer usageCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID createdBy;
}