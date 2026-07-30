package com.avionics_systems.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestRunRequest {
    private UUID testId;
    private UUID executionId;
    private UUID projectId;
    private UUID executedBy;
    private String environment;
    private String browser;
    private String platform;
    private String testData;
    private List<String> evidenceLinks;
}