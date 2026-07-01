package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataTransformRequest {

    @NotNull(message = "Dataset ID is required")
    private UUID datasetId;

    private UUID projectId;

    // Transformation operations to apply
    private List<TransformOperation> operations;

    // Output settings
    private String outputFormat; // TABULAR, JSON, CSV
    private Boolean createNewDataset; // true = create new, false = return data
    private String newDatasetName; // name for new dataset if createNewDataset = true

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransformOperation {
        private String operationType; // FILTER, MAP, AGGREGATE, SORT, PROJECT, JOIN, CALCULATE

        // Filter operation
        private String filterColumn;
        private String filterOperator; // EQ, NE, GT, GTE, LT, LTE, IN, NOT_IN, LIKE, NOT_LIKE, IS_NULL, IS_NOT_NULL
        private Object filterValue;

        // Map/Project operation
        private List<String> selectColumns; // columns to keep

        // Aggregate operation
        private String groupByColumn;
        private String aggregateColumn;
        private String aggregateFunction; // COUNT, SUM, AVG, MIN, MAX, DISTINCT_COUNT

        // Sort operation
        private String sortColumn;
        private Boolean ascending = true;

        // Calculate operation
        private String newColumnName;
        private String expression; // e.g., "col1 + col2", "col1 * 2"

        // Join operation
        private UUID joinDatasetId;
        private String leftColumn;
        private String rightColumn;
        private String joinType; // INNER, LEFT, RIGHT, FULL
    }
}