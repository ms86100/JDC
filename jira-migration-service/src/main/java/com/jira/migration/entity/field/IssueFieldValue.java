package com.jira.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Stores the actual field values for issues.
 * Supports both built-in and custom field values in a unified schema.
 */
@Entity
@Table(name = "issue_field_values", schema = "jira_migration",
        uniqueConstraints = @UniqueConstraint(columnNames = {"issue_id", "field_definition_id"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "field_definition_id", nullable = false)
    private UUID fieldDefinitionId;

    @Column(name = "string_value", columnDefinition = "TEXT")
    private String stringValue;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "integer_value")
    private Integer integerValue;

    @Column(name = "long_value")
    private Long longValue;

    @Column(name = "double_value")
    private Double doubleValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "datetime_value")
    private LocalDateTime datetimeValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "array_value", columnDefinition = "jsonb")
    private List<Object> arrayValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "object_value", columnDefinition = "jsonb")
    private Map<String, Object> objectValue;

    @Column(name = "formatted_value", columnDefinition = "TEXT")
    private String formattedValue;

    @Column(name = "raw_value", columnDefinition = "TEXT")
    private String rawValue;

    @Column(name = "value_source", length = 100)
    private String valueSource;

    @Column(name = "value_hash", length = 64)
    private String valueHash;

    @Column(name = "validation_status", length = 50)
    @Builder.Default
    private String validationStatus = "VALID";

    @Column(name = "validation_message", columnDefinition = "TEXT")
    private String validationMessage;

    @Column(name = "imported_from", length = 100)
    private String importedFrom;

    @Column(name = "import_mapping_id")
    private UUID importMappingId;

    @Column(name = "searchable_text", columnDefinition = "TEXT")
    private String searchableText;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_definition_id", insertable = false, updatable = false)
    private FieldDefinition fieldDefinition;

    public enum ValidationStatus {
        VALID,
        INVALID,
        WARNING,
        PENDING,
        UNMAPPED
    }
}