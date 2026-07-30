package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.UUID;

/**
 * Request DTO for creating a test repository folder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderRequest {

    private UUID parentFolderId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean isSmartFolder;
    private String smartFolderQuery;
}