package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence_retention_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceRetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_type", length = 50)
    private String evidenceType; // SCREENSHOT, VIDEO, LOG, etc.

    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 365;

    @Column(name = "compression_enabled")
    @Builder.Default
    private Boolean compressionEnabled = false;

    @Column(name = "auto_archive")
    @Builder.Default
    private Boolean autoArchive = true;

    @Column(name = "move_to_cold_storage")
    @Builder.Default
    private Boolean moveToColdStorage = false;

    @Column(name = "cold_storage_after_days")
    @Builder.Default
    private Integer coldStorageAfterDays = 90;

    @Column(name = "permanent_delete")
    @Builder.Default
    private Boolean permanentDelete = false;

    @Column(name = "delete_after_days")
    private Integer deleteAfterDays; // If permanentDelete is true

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}