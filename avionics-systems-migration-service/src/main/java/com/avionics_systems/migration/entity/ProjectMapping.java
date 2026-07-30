package com.avionics_systems.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "project_mappings", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "source_key", nullable = false, length = 10)
    private String sourceKey;

    @Column(name = "target_key", length = 10)
    private String targetKey;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "issue_key_sequence")
    @Builder.Default
    private Integer issueKeySequence = 0; // Current issue number for this project

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "component_mappings", columnDefinition = "jsonb")
    private Map<String, Object> componentMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "version_mappings", columnDefinition = "jsonb")
    private Map<String, Object> versionMappings;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}