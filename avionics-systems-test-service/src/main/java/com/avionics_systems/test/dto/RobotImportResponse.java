package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RobotImportResponse {
    private UUID batchId;
    private String status;
    private int totalTests;
    private int passed;
    private int failed;
    private int skipped;
    private String message;
    private List<TestResponse> createdTests;
}
