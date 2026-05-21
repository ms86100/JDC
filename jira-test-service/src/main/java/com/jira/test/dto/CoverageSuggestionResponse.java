package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageSuggestionResponse {
    private UUID projectId;
    private List<CoverageSuggestion> suggestions;
    private List<PrioritizedTest> prioritizedTests;
    private AutomatedActionSummary actionSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageSuggestion {
        private String type;
        private String priority;
        private String requirementKey;
        private String description;
        private int estimatedImpact;
        private List<String> suggestedTestIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrioritizedTest {
        private UUID testId;
        private String testKey;
        private String requirementKey;
        private int priorityScore;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutomatedActionSummary {
        private int totalSuggestions;
        private int highPriorityCount;
        private int mediumPriorityCount;
        private int lowPriorityCount;
        private BigDecimal potentialCoverageGain;
    }
}