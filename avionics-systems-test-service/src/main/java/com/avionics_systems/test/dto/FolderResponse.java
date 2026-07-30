package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private UUID parentId;
    private String folderType;
    private String path;
    private Integer depth;
    private Integer sortOrder;
    private String status;
    private UUID ownerId;
    private String icon;
    private String color;
    private Boolean isStarred;
    private Boolean isExpanded;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FolderResponse> children;
    private Integer testCount;
    private Integer childCount;
}