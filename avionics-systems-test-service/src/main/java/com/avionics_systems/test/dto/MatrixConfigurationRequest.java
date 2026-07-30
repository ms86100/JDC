package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatrixConfigurationRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Matrix name is required")
    private String name;

    private String description;

    @NotNull(message = "Dimension configs are required")
    private List<DimensionConfig> dimensions;

    private List<FilterRule> filterRules;

    private List<ConflictRule> conflictRules;

    private UUID createdBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DimensionConfig {
        private String name; // Browser, OS, Region, etc.
        private List<String> values; // Chrome, Firefox, Safari
        private String type; // SINGLE_SELECT, MULTI_SELECT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FilterRule {
        private String type; // INCLUDE, EXCLUDE
        private String dimension;
        private List<String> values;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConflictRule {
        private String ruleName;
        private String type; // INCOMPATIBLE, MUTUALLY_EXCLUSIVE
        private Map<String, List<String>> conflicts; // {browser: ["IE"], os: ["Linux"]}
    }
}