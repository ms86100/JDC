package com.jira.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "field_configuration_overrides", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigurationOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_type_id")
    private UUID issueTypeId;

    @Column(name = "field_key", nullable = false, length = 255)
    private String fieldKey;

    @Column(name = "hidden")
    @Builder.Default
    private Boolean hidden = false;

    @Column(name = "required")
    @Builder.Default
    private Boolean required = false;

    @Column(name = "read_only")
    @Builder.Default
    private Boolean readOnly = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
