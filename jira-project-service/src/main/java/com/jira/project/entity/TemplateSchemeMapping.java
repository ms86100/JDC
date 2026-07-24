package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Template Scheme Mapping - Associates templates with schemes (issue type, workflow, permission, etc.)
 */
@Entity
@Table(name = "template_scheme_mappings", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSchemeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "scheme_type", nullable = false, length = 50)
    private String schemeType;

    @Column(name = "scheme_name", nullable = false, length = 100)
    private String schemeName;

    @Column(name = "scheme_id")
    private UUID schemeId;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Scheme types
    public static final String SCHEME_TYPE_ISSUE_TYPE = "ISSUE_TYPE";
    public static final String SCHEME_TYPE_WORKFLOW = "WORKFLOW";
    public static final String SCHEME_TYPE_PERMISSION = "PERMISSION";
    public static final String SCHEME_TYPE_NOTIFICATION = "NOTIFICATION";
    public static final String SCHEME_TYPE_SCREEN = "SCREEN";
}