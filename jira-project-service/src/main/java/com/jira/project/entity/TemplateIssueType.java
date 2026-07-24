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
 * Template Issue Type - Associates issue types with project templates
 */
@Entity
@Table(name = "template_issue_types", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateIssueType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "issue_type_name", nullable = false, length = 50)
    private String issueTypeName;

    @Column(name = "issue_type_icon", length = 50)
    private String issueTypeIcon;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_subtask")
    @Builder.Default
    private Boolean isSubtask = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}