package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionDiffResponse {

    private UUID datasetId;
    private String datasetName;
    private Integer fromVersion;
    private Integer toVersion;

    private Boolean hasChanges;
    private ChangeSummary summary;

    // Detailed changes
    private List<ColumnChange> columnChanges;
    private List<RowChange> rowChanges;

    // Data summaries
    private DataSummary fromVersionSummary;
    private DataSummary toVersionSummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangeSummary {
        private Integer columnsAdded;
        private Integer columnsRemoved;
        private Integer columnsModified;
        private Integer rowsAdded;
        private Integer rowsRemoved;
        private Integer rowsModified;
        private Integer cellsModified;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnChange {
        private String columnName;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private String typeFrom;
        private String typeTo;
        private Integer positionFrom;
        private Integer positionTo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowChange {
        private Integer rowIndex;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private List<String> modifiedColumns;
        private List<String> oldValues;
        private List<String> newValues;
        private String matchReason; // For removed/added rows - why matched or not
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataSummary {
        private Integer rowCount;
        private Integer columnCount;
        private List<String> columnNames;
        private List<String> columnTypes;
    }
}