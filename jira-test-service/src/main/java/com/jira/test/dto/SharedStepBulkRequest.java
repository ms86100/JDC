package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepBulkRequest {

    private List<UUID> sharedStepIds;

    private String operation; // UPDATE, DELETE, ARCHIVE, TAG, MIGRATE, EXPORT

    // Update operation fields
    private String newName;
    private String newDescription;
    private UUID newFolderId;

    // Tag operation fields
    private List<String> addTags;
    private List<String> removeTags;
    private List<String> addCategories;

    // Migrate operation fields
    private Integer targetVersion;

    // Export operation fields
    private String exportFormat; // JSON, YAML

    // Options
    private Boolean skipValidation;
    private Boolean forceOperation; // Force even with warnings
}