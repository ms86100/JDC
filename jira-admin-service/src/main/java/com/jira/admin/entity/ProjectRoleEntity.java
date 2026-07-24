package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_roles", schema = "jira_admin")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "role_type", length = 50)
    private String roleType;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean defaultRole = false;

    @Column(name = "permissions", columnDefinition = "JSONB")
    private String permissions;

    @Column(name = "is_system_role")
    @Builder.Default
    private Boolean systemRole = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}