package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for JUnit XML import
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JunitImportResponse {

    private UUID batchId;
    private Integer totalTests;
    private Integer passed;
    private Integer failed;
    private Integer skipped;
    private Integer executionsCreated;
    private String status;
    private String errorMessage;
}