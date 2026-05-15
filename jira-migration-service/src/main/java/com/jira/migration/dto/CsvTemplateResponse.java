package com.jira.migration.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvTemplateResponse {
    private UUID id;
    private String templateName;
    private String entityType;
    private String version;
    private List<ColumnDefinition> columns;
    private Integer headerRow;
    private Integer dataStartRow;
    private List<ValidationRule> validationRules;
    private String fieldMapping;
    private Boolean supportsBulkImport;
    private Integer maxRowsPerFile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDefinition {
        private String columnName;
        private String displayName;
        private String dataType;
        private Integer maxLength;
        private Boolean required;
        private String pattern;
        private List<String> allowedValues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRule {
        private String field;
        private List<String> rules;
    }
}