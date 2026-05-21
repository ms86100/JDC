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
public class DatasetValidationResponse {

    private UUID datasetId;
    private String datasetName;
    private Boolean isValid;
    private LocalDateTime validatedAt;

    private Integer totalRows;
    private Integer totalColumns;
    private Integer validRows;
    private Integer invalidRows;

    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private List<ValidationInfo> infos;

    private Map<String, ColumnValidationResult> columnResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private Integer rowIndex;
        private String columnName;
        private String fieldName;
        private String message;
        private String ruleType;
        private String actualValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationWarning {
        private Integer rowIndex;
        private String columnName;
        private String message;
        private String severity; // LOW, MEDIUM, HIGH
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationInfo {
        private String columnName;
        private String message;
        private Integer count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnValidationResult {
        private String columnName;
        private String dataType;
        private Integer totalValues;
        private Integer nullValues;
        private Integer uniqueValues;
        private Double completenessPercent;
        private List<String> sampleValues;
        private Boolean hasErrors;
        private Integer errorCount;
    }
}