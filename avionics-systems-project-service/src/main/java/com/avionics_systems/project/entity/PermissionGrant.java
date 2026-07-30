package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permission Grant - Legacy DC Compatible
 *
 * Represents a single permission grant to a user, group, or project role.
 */
@Entity
@Table(name = "permission_grants", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    private PermissionScheme permissionScheme;

    /**
     * Grant type determines how the permission is applied:
     * - USER: Granted to a specific user
     * - GROUP: Granted to all users in a group
     * - PROJECT_ROLE: Granted to users in a project role
     */
    @Column(name = "grant_type", nullable = false, length = 20)
    private String grantType;

    public static final String TYPE_USER = "USER";
    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_PROJECT_ROLE = "PROJECT_ROLE";

    // For USER grants
    @Column(name = "entity_id")
    private UUID entityId;

    // For GROUP grants
    @Column(name = "group_name", length = 100)
    private String groupName;

    // For PROJECT_ROLE grants
    @Column(name = "project_role_id")
    private UUID projectRoleId;

    // The permission being granted (references permissions.key_name)
    @Column(name = "permission_key", nullable = false, length = 50)
    private String permissionKey;

    // Optional: applies to specific issue only
    @Column(name = "issue_id")
    private UUID issueId;

    // Optional: applies to issues at this security level or below
    @Column(name = "issue_security_level_id")
    private UUID issueSecurityLevelId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Builder helper for user grants
    public static PermissionGrant forUser(UUID userId, String permissionKey) {
        return PermissionGrant.builder()
            .grantType(TYPE_USER)
            .entityId(userId)
            .permissionKey(permissionKey)
            .build();
    }

    // Builder helper for group grants
    public static PermissionGrant forGroup(String groupName, String permissionKey) {
        return PermissionGrant.builder()
            .grantType(TYPE_GROUP)
            .groupName(groupName)
            .permissionKey(permissionKey)
            .build();
    }

    // Builder helper for project role grants
    public static PermissionGrant forRole(UUID roleId, String permissionKey) {
        return PermissionGrant.builder()
            .grantType(TYPE_PROJECT_ROLE)
            .projectRoleId(roleId)
            .permissionKey(permissionKey)
            .build();
    }
}