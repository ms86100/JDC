package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CucumberImportResponse {
    private UUID batchId;
    private String status;
    private int totalScenarios;
    private int importedTests;
    private int skippedScenarios;
    private List<String> errors;
    private List<TestResponse> createdTests;
}