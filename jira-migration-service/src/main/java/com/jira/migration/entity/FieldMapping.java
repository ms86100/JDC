package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "field_mappings", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mapping_name", nullable = false, length = 100)
    private String mappingName;

    @Column(name = "mapping_type", nullable = false, length = 30)
    private String mappingType; // IMPORT, EXPORT

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // JIRA_DC, CSV, etc.

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // JIRA_PLATFORM

    // Mapping definition - Array of {source_field, target_field, default_value, transformer}
    @Column(name = "mappings", nullable = false, columnDefinition = "jsonb")
    private String mappings;

    @Column(name = "sample_data", columnDefinition = "jsonb")
    private String sampleData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_shared")
    @Builder.Default
    private Boolean isShared = false;
}