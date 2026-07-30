package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestImpactDto {
    private String testId;
    private String testIssueKey;
    private String testName;
    private String impactLevel; // HIGH, MEDIUM, LOW
    private Double riskScore;
    private String reason;

    // Static factory for converting UUID to String
    public static TestImpactDto from(UUID uuid, String issueKey, String name) {
        return TestImpactDto.builder()
                .testId(uuid != null ? uuid.toString() : null)
                .testIssueKey(issueKey)
                .testName(name)
                .build();
    }
}