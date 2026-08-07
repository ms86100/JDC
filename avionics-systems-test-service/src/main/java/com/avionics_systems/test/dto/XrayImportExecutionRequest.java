package com.avionics_systems.test.dto;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class XrayImportExecutionRequest {
    private String testExecutionKey;
    private XrayExecutionInfo info;
    private List<XrayTestResult> tests;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class XrayExecutionInfo {
        private String summary;
        private String description;
        private String startDate;
        private String finishDate;
        private String testPlanKey;
        private List<String> testEnvironments;
        private String revision;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class XrayTestResult {
        private String testKey;
        private String status;
        private String comment;
        private String start;
        private String finish;
        private String executedBy;
        private String assignee;
        private List<XrayStepResult> steps;
        private List<XrayEvidence> evidences;
        private List<String> defects;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class XrayStepResult {
        private String status;
        private String comment;
        private String actualResult;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class XrayEvidence {
        private String data;
        private String filename;
        private String contentType;
    }
}
