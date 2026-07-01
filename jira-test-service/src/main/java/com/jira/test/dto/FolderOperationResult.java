package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderOperationResult {
    private UUID folderId;
    private String folderName;
    private Boolean success;
    private String message;
}
