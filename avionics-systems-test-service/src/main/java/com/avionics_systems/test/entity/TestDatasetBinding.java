package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_dataset_bindings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDatasetBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(name = "dataset_version_id")
    private UUID datasetVersionId;

    @Column(name = "column_mappings", columnDefinition = "JSONB")
    private String columnMappings; // JSON: {"username": "${username}", "password": "${password}"}

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}