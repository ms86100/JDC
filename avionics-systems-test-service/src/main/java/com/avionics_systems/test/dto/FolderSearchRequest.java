package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderSearchRequest {

    private UUID projectId;

    private String query;

    private List<UUID> folderIds;

    private UUID parentId;

    private String folderType;

    private List<String> tags;

    private Integer minDepth;
    private Integer maxDepth;

    private String pathPrefix;

    private String status;

    private UUID ownerId;

    private Boolean starredOnly;

    private Boolean hasTests;

    private Integer page;
    private Integer pageSize;
    private String sortBy;
    private String sortDirection;
}