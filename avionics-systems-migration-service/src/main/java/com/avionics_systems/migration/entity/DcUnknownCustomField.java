package com.avionics_systems.migration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dc_unknown_custom_fields", schema = "jira_migration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DcUnknownCustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "field_id", nullable = false, length = 100)
    private String fieldId;

    @Column(name = "field_name", length = 255)
    private String fieldName;

    @Column(name = "sample_value", columnDefinition = "TEXT")
    private String sampleValue;

    @Column(name = "detected_type", length = 50)
    private String detectedType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
