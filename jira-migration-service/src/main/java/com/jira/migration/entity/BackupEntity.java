package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_entities", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "backup_id", nullable = false)
    private UUID backupId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_key", length = 255)
    private String entityKey;

    @Column(name = "entity_data", nullable = false, columnDefinition = "jsonb")
    private String entityData; // Full entity snapshot

    @Column(name = "dependencies", columnDefinition = "jsonb")
    private String dependencies; // Entity keys this depends on

    @Column(name = "parent_key", length = 255)
    private String parentKey; // For hierarchy support

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}