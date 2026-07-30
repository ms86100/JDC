package com.avionics_systems.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "csv_templates", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PROJECT, ISSUE, USER, etc.

    @Column(length = 20)
    @Builder.Default
    private String version = "1.0";

    // Template definition - columns JSONB
    @Column(name = "columns", nullable = false, columnDefinition = "jsonb")
    private String columns;

    @Column(name = "header_row")
    @Builder.Default
    private Integer headerRow = 1;

    @Column(name = "data_start_row")
    @Builder.Default
    private Integer dataStartRow = 2;

    // Validation rules
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private String validationRules;

    @Column(name = "custom_validators", columnDefinition = "jsonb")
    private String customValidators;

    // Field mapping to Legacy DC
    @Column(name = "field_mapping", columnDefinition = "jsonb")
    private String fieldMapping;

    // Options
    @Column(name = "supports_bulk_import")
    @Builder.Default
    private Boolean supportsBulkImport = true;

    @Column(name = "max_rows_per_file")
    @Builder.Default
    private Integer maxRowsPerFile = 50000;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;
}