package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockDataResponse {

    private Boolean success;
    private UUID datasetId; // If created as dataset
    private String datasetName;

    private Integer rowCount;
    private Integer columnCount;
    private List<String> columnNames;
    private List<String> columnTypes;

    private List<List<String>> rows;
    private String outputFormat;

    private Long seedUsed;
    private String locale;
    private Long generationTimeMs;

    private List<String> warnings;
    private Map<String, Object> metadata;
}