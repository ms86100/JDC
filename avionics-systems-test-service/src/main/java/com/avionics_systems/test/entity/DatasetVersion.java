package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "data", columnDefinition = "JSONB", nullable = false)
    private String data;

    @Column(name = "column_names", columnDefinition = "TEXT[]")
    private String[] columnNames;

    @Column(name = "column_types", columnDefinition = "TEXT[]")
    private String[] columnTypes;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_immutable")
    @Builder.Default
    private Boolean isImmutable = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}