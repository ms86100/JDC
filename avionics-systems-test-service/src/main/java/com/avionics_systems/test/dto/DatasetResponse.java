package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String dataFormat;
    private List<String> columnNames;
    private List<String> columnTypes;
    private Integer rowCount;
    private Integer version;
    private Boolean isImmutable;
    private UUID folderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional computed fields
    private List<List<String>> rows; // Actual data rows
    private Integer totalVersions;
    private List<DatasetVersionResponse> versions;
}