package com.avionics_systems.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactAnalysisRequest {

    private UUID projectId;

    private String triggerType; // commit, pr, manual, schedule

    private String commitSha;

    private String commitMessage;

    private String[] changedFiles;

    private String prId;

    private String branch;
}