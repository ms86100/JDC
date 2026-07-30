package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderAccessRecord {
    private UUID folderId;
    private String folderName;
    private UUID projectId;
    private String path;
    private LocalDateTime accessedAt;
    private LocalDateTime modifiedAt;
    private UUID accessedBy;
}
