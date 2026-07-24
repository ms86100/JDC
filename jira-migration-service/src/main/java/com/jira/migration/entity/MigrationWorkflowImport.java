package com.jira.migration.entity;

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
@Table(name = "migration_workflow_imports", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationWorkflowImport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "source_format", nullable = false)
    private String sourceFormat;

    @Column(name = "target_workflow_id")
    private String targetWorkflowId;

    @Column(name = "target_scheme_id")
    private String targetSchemeId;

    @Column(name = "import_status", nullable = false)
    private String importStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "descriptor_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> descriptorJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scheme_json", columnDefinition = "jsonb")
    private Map<String, Object> schemeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "graph_json", columnDefinition = "jsonb")
    private Map<String, Object> graphJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_report", columnDefinition = "jsonb")
    private Map<String, Object> validationReport;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "simulation_trace", columnDefinition = "jsonb")
    private Map<String, Object> simulationTrace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_before", columnDefinition = "jsonb")
    private Map<String, Object> snapshotBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unsupported_features", columnDefinition = "jsonb")
    private Map<String, Object> unsupportedFeatures;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;
}
