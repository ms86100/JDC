package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinationResponse {

    private UUID id;
    private UUID matrixId;
    private Integer combinationIndex;
    private Map<String, String> combinationData; // {browser: "Chrome", os: "Windows"}
    private Boolean isValid;
    private List<ValidationError> validationErrors;
    private Map<String, Object> provisionedConfig;
    private String provisioningStatus;
    private LocalDateTime provisionedAt;
    private String provisioningError;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String rule;
        private String details;
        private String affectedDimensions;
    }
}