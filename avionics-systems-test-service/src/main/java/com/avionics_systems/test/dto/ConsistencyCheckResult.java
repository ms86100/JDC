package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsistencyCheckResult {

    private UUID projectId;
    private int totalVvosChecked;
    private int totalTestsChecked;
    private int totalInconsistencies;
    private List<ConsistencyItem> items;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsistencyItem {
        private UUID vvoId;
        private String vvoIssueKey;
        private String vvoSummary;
        private UUID testId;
        private String testName;
        private String fieldName;
        private String vvoValue;
        private String testValue;
        private String severity; // WARNING, ERROR
        private String message;
    }
}
