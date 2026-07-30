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
public class MockDataRequest {

    private UUID projectId;

    @NotNull(message = "Schema is required")
    private List<ColumnSchema> schema;

    private Integer rowCount = 10;
    private String outputFormat = "TABULAR"; // TABULAR, JSON, CSV

    // Optional seed for reproducibility
    private Long seed;

    // Locale for data generation
    private String locale = "en_US";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnSchema {
        private String name;
        private String type; // STRING, NUMBER, BOOLEAN, DATE, DATETIME, EMAIL, URL, PHONE, ADDRESS, NAME, UUID, CUSTOM

        // For STRING type
        private String pattern; // e.g., "US-XXXX-XXXX" for phone-like
        private Integer minLength;
        private Integer maxLength;
        private List<String> possibleValues; // For enumerated types

        // For NUMBER type
        private Double min;
        private Double max;
        private Integer decimalPlaces;
        private Boolean allowNegative;

        // For DATE/DATETIME type
        private String minDate; // ISO format
        private String maxDate; // ISO format

        // For CUSTOM type
        private String customGenerator; // Class name of custom generator
        private Map<String, String> customParams;

        // Distribution settings
        private String distribution; // UNIFORM, NORMAL, ZIPF, SEQUENTIAL
        private Double distributionParam1;
        private Double distributionParam2;

        // Null handling
        private Double nullPercentage = 0.0; // 0.0 to 1.0

        // Uniqueness
        private Boolean unique = false;
    }
}