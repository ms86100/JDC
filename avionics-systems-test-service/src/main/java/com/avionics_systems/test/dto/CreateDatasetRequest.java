package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDatasetRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Dataset name is required")
    private String name;

    private String description;

    private String dataFormat; // TABULAR, CSV, JSON

    private List<String> columnNames;

    private List<String> columnTypes; // STRING, NUMBER, BOOLEAN, SECRET

    private List<List<String>> rows; // For tabular data

    private String csvData; // Raw CSV string

    private String jsonData; // Raw JSON string

    private UUID folderId;
}