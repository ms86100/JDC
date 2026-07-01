package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_datasets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_format", length = 50)
    @Builder.Default
    private String dataFormat = "TABULAR"; // TABULAR, CSV, JSON

    @Column(name = "column_names", columnDefinition = "TEXT[]")
    private List<String> columnNames;

    @Column(name = "column_types", columnDefinition = "TEXT[]")
    private List<String> columnTypes; // STRING, NUMBER, BOOLEAN, SECRET

    @Column(name = "data", columnDefinition = "JSONB")
    private String data; // JSON representation of dataset rows

    @Column(name = "csv_data", columnDefinition = "TEXT")
    private String csvData; // Raw CSV data if imported from CSV

    @Column(name = "row_count")
    @Builder.Default
    private Integer rowCount = 0;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "is_immutable")
    @Builder.Default
    private Boolean isImmutable = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column
    @Builder.Default
    private Boolean archived = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}