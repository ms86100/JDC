package com.jira.portal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_types", schema = "jira_portal",
    indexes = {
        @Index(name = "idx_request_type_portal_id", columnList = "portal_id"),
        @Index(name = "idx_request_type_issue_type", columnList = "issue_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portal_id", nullable = false)
    private UUID portalId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "issue_type", nullable = false, length = 100)
    private String issueType; // Bug, Feature, Support, etc.

    @Column(name = "issue_type_id")
    private UUID issueTypeId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(columnDefinition = "TEXT")
    private String fieldsConfig; // JSON configuration for request form fields

    @Column(columnDefinition = "TEXT")
    private String instructions; // Instructions shown to customer before submission

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "sla_minutes")
    @Builder.Default
    private Integer slaMinutes = 480; // Default 8 hours

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "workflow_id")
    private UUID workflowId; // For automatic workflow transitions
}