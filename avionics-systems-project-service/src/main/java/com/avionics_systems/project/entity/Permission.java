package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.util.UUID;

/**
 * Permission Entity - Legacy DC Compatible
 *
 * Represents a permission that can be granted within the system.
 * Matches Legacy DC's permission system.
 */
@Entity
@Table(name = "permissions", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Category of permission:
     * - PROJECT: Permissions related to project access
     * - ISSUE: Permissions related to issue operations
     * - ADMIN: Administrative permissions
     * - GLOBAL: System-wide permissions
     */
    @Column(nullable = false, length = 30)
    private String category;

    /**
     * Short key identifier (e.g., "BROWSE_PROJECTS", "EDIT_ISSUES")
     */
    @Column(name = "key_name", nullable = false, length = 30)
    private String keyName;

    // Permission categories
    public static final String CATEGORY_PROJECT = "PROJECT";
    public static final String CATEGORY_ISSUE = "ISSUE";
    public static final String CATEGORY_ADMIN = "ADMIN";
    public static final String CATEGORY_GLOBAL = "GLOBAL";

    // Common permission keys
    public static final String BROWSE_PROJECTS = "BROWSE_PROJECTS";
    public static final String CREATE_PROJECTS = "CREATE_PROJECTS";
    public static final String ADMINISTER_PROJECTS = "ADMINISTER_PROJECTS";
    public static final String CREATE_ISSUES = "CREATE_ISSUES";
    public static final String EDIT_ISSUES = "EDIT_ISSUES";
    public static final String DELETE_ISSUES = "DELETE_ISSUES";
    public static final String ASSIGN_ISSUES = "ASSIGN_ISSUES";
    public static final String RESOLVE_ISSUES = "RESOLVE_ISSUES";
    public static final String CREATE_COMMENTS = "CREATE_COMMENTS";
    public static final String CREATE_ATTACHMENTS = "CREATE_ATTACHMENTS";
    public static final String WORK_ON_ISSUES = "WORK_ON_ISSUES";
}
