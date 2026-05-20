package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDatasetRequest {

    private String name;

    private String description;

    private List<String> columnNames;

    private List<String> columnTypes;

    private List<List<String>> rows;

    private String csvData;

    private String jsonData;

    private Boolean isImmutable;
}