package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetSharingResponse {

    private Boolean success;
    private UUID sourceDatasetId;
    private UUID targetDatasetId;
    private String targetDatasetName;

    private UUID targetProjectId;
    private LocalDateTime sharedAt;

    private List<String> appliedRoles;
    private Boolean isPublic;

    private Integer totalRows;
    private Integer versionIncluded;
    private Boolean bindingsIncluded;

    private String shareLink; // If sharing via link
}