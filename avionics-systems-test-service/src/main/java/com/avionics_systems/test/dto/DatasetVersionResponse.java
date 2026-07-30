package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetVersionResponse {

    private UUID id;
    private UUID datasetId;
    private Integer versionNumber;
    private List<String> columnNames;
    private List<String> columnTypes;
    private List<List<String>> data;
    private Integer rowCount;
    private String changeSummary;
    private UUID createdBy;
    private Boolean isImmutable;
    private LocalDateTime createdAt;
}