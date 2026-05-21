package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetExportRequest {

    private UUID datasetId;
    private UUID projectId;

    // Export format
    private String format; // CSV, JSON, XML, EXCEL, SQL, YAML

    // Data options
    private List<String> columnsToInclude; // null = all columns
    private Integer rowLimit; // null = all rows
    private Integer offset; // for pagination

    // Formatting options
    private Boolean includeHeaders = true;
    private String delimiter = ","; // For CSV
    private String quoteChar = "\""; // For CSV
    private String dateFormat = "yyyy-MM-dd";
    private String datetimeFormat = "yyyy-MM-dd'T'HH:mm:ss";
    private Boolean prettyPrint = true; // For JSON

    // Metadata options
    private Boolean includeMetadata = true;
    private Boolean includeSchema = true; // For JSON, XML
    private Boolean includeRowNumbers = false;

    // Compression
    private Boolean compress = false;
    private String compressionFormat; // ZIP, GZIP

    // Custom transformations before export
    private Map<String, String> columnMappings; // Rename columns in export
    private List<String> computedColumns; // Add computed columns
}