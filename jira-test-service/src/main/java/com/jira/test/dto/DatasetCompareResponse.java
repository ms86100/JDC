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
public class DatasetCompareResponse {

    private UUID datasetId1;
    private String dataset1Name;
    private UUID datasetId2;
    private String dataset2Name;

    private Boolean areIdentical;
    private LocalDateTime comparedAt;

    // Structure comparison
    private StructureComparison structureComparison;

    // Data comparison
    private DataComparison dataComparison;

    // Summary
    private Integer totalDifferences;
    private Double similarityPercent;
    private List<String> differenceSummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StructureComparison {
        private Boolean hasSameColumns;
        private Boolean hasSameColumnOrder;
        private Boolean hasSameColumnTypes;
        private Integer columnCount1;
        private Integer columnCount2;

        private List<String> columnsOnlyInDataset1;
        private List<String> columnsOnlyInDataset2;
        private List<String> columnsWithDifferentTypes;

        private List<MapDifference> columnTypeDifferences;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataComparison {
        private Integer rowCount1;
        private Integer rowCount2;
        private Integer matchingRows;
        private Integer differentRows;
        private Integer rowsOnlyInDataset1;
        private Integer rowsOnlyInDataset2;

        private List<RowDifference> rowDifferences;
        private List<CellDifference> cellDifferences;

        private Map<String, Integer> columnDiffCounts; // column name -> count of differences
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MapDifference {
        private String columnName;
        private String typeInDataset1;
        private String typeInDataset2;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowDifference {
        private Integer rowIndex1;
        private Integer rowIndex2;
        private String differenceType; // MODIFIED, ADDED, REMOVED
        private List<String> differingColumns;
        private Integer differingCellCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CellDifference {
        private String columnName;
        private Integer rowIndex;
        private String valueInDataset1;
        private String valueInDataset2;
        private Double differenceMagnitude; // For numeric values
    }
}