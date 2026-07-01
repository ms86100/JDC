package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id")
    private String projectId;

    private String roleType;

    @Column(name = "default_role")
    private Boolean defaultRole = false;
}