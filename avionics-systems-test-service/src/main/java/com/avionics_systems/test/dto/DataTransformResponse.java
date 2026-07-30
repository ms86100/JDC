package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataTransformResponse {

    private Boolean success;
    private UUID sourceDatasetId;
    private UUID resultDatasetId; // if created as new dataset
    private String resultDatasetName;

    private Integer originalRowCount;
    private Integer resultRowCount;
    private Integer originalColumnCount;
    private Integer resultColumnCount;

    private List<List<String>> rows;
    private List<String> columnNames;
    private List<String> columnTypes;

    private List<String> appliedOperations;
    private String outputFormat;

    private List<String> warnings;
    private List<String> errors;
}