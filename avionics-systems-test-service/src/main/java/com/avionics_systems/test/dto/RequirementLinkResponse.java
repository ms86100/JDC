package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementLinkResponse {

    private UUID id;
    private String requirementKey;
    private String requirementType;
    private UUID testId;
    private String coverageStatus;
    private LocalDateTime createdAt;
}