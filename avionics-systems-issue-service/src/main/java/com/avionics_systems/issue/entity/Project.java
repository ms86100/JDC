package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_key", unique = true, nullable = false, length = 10)
    private String projectKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_type", length = 50)
    private String projectType;

    @Column(name = "lead_user_id")
    private UUID leadUserId;

    @Column(name = "default_assignee_id")
    private UUID defaultAssigneeId;

    @Column(name = "allow_attachments")
    private Boolean allowAttachments = true;

    @Column(name = "default_issue_security_level")
    private UUID defaultIssueSecurityLevel;

    @Column(name = "default_priority")
    private String defaultPriority;

    @Column(name = "default_resolution")
    private String defaultResolution;

    @Column(name = "issue_number_counter")
    private Long issueNumberCounter = 1L;

    @Column(name = "default_language", length = 10)
    private String defaultLanguage = "en";

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
