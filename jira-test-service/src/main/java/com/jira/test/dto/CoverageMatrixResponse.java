package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageMatrixResponse {
    private UUID projectId;
    private List<String> requirementKeys;
    private List<String> testSetNames;
    private List<List<MatrixCell>> matrix;
    private MatrixSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixCell {
        private String requirementKey;
        private String testSetName;
        private BigDecimal coverage;
        private int testsCovered;
        private int totalTests;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixSummary {
        private BigDecimal overallCoverage;
        private int totalRequirements;
        private int totalTestSets;
        private int fullyCoveredRequirements;
        private int partiallyCoveredRequirements;
        private int uncoveredRequirements;
    }
}