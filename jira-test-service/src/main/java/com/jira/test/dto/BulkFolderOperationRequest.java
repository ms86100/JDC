package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkFolderOperationRequest {

    private List<UUID> folderIds;

    private String operationType;

    private UUID targetParentId;

    private Map<String, Object> updateFields;

    private List<String> tagsToAdd;
    private List<String> tagsToRemove;

    private Boolean recursive;
}