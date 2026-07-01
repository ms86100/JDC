package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetSharingRequest {

    private UUID sourceDatasetId;
    private UUID targetProjectId;

    // Sharing options
    private Boolean shareAsCopy = true; // true = copy, false = reference
    private Boolean includeHistory = false;
    private Boolean includeBindings = true;

    // Optional name for shared dataset
    private String targetDatasetName;

    // Permissions for shared dataset
    private List<String> targetRoles; // Roles that can access
    private Boolean makePublic = false;
}