package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TestDataset - Data tables for data-driven testing
 */
@Entity
@Table(name = "test_datasets", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_td_project", columnList = "project_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_format", length = 50)
    @Builder.Default
    private String dataFormat = "TABLE"; // TABLE, CSV, JSON, EXCEL

    @Column(name = "column_names", columnDefinition = "text[]")
    private String[] columnNames; // Column headers

    @Column(name = "column_types", columnDefinition = "text[]")
    private String[] columnTypes; // Data types: STRING, NUMBER, DATE, BOOLEAN

    @Column(name = "rows_count")
    @Builder.Default
    private Integer rowsCount = 0;

    // Data stored as JSON array for flexibility
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<List<String>> data; // 2D array of values

    @Column(name = "csv_data", columnDefinition = "TEXT")
    private String csvData; // Raw CSV data

    // Usage tracking
    @Column(name = "used_in_tests")
    @Builder.Default
    private Integer usedInTests = 0;

    @Column(name = "used_in_automations")
    @Builder.Default
    private Integer usedInAutomations = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}