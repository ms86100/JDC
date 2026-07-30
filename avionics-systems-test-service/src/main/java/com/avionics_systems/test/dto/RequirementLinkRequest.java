package com.avionics_systems.test.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementLinkRequest {
    private String requirementKey;
    private String requirementType;
    private java.util.UUID testId;
    private String coverageStatus;
}