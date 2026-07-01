package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "wizard_sessions", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WizardSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "current_step", nullable = false, length = 50)
    @Builder.Default
    private String currentStep = "UPLOAD";

    @Column(name = "import_type", nullable = false, length = 50)
    private String importType;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "target_project_id")
    private UUID targetProjectId;

    @Column(name = "migration_job_id")
    private UUID migrationJobId;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_headers", columnDefinition = "jsonb")
    private List<String> detectedHeaders;

    @Column(name = "detected_entity_type", length = 50)
    private String detectedEntityType;

    @Column(name = "attachment_column", length = 100)
    private String attachmentColumn;

    @Column(name = "parent_column", length = 100)
    private String parentColumn;

    @Column(name = "epic_column", length = 100)
    private String epicColumn;

    @Column(name = "total_rows")
    @Builder.Default
    private Integer totalRows = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", columnDefinition = "jsonb")
    private Map<String, Object> validationResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mappings", columnDefinition = "jsonb")
    private List<Map<String, Object>> fieldMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_mappings", columnDefinition = "jsonb")
    private List<Map<String, Object>> userMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_mappings", columnDefinition = "jsonb")
    private List<Map<String, Object>> optionMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "workflow_status_mappings", columnDefinition = "jsonb")
    private Map<String, Object> workflowStatusMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_defaults", columnDefinition = "jsonb")
    private Map<String, Object> fieldDefaults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "import_options", columnDefinition = "jsonb")
    private Map<String, Object> importOptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_data", columnDefinition = "jsonb")
    private Map<String, Object> sessionData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preview_rows", columnDefinition = "jsonb")
    private List<List<String>> previewRows;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public void advanceStep(String step) {
        this.currentStep = step;
    }
}
