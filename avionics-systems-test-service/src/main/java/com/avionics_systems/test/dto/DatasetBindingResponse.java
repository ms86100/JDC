package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetBindingResponse {

    private UUID id;
    private UUID testId;
    private String testIssueKey;
    private UUID datasetId;
    private String datasetName;
    private UUID datasetVersionId;
    private Integer datasetVersion;
    private List<String> boundColumns;
    private Integer rowCount;
    private String createdBy;
    private String createdAt;
}