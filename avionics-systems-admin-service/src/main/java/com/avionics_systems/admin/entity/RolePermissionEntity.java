package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_permissions")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "project_role_id", nullable = false)
    private String projectRoleId;

    @Column(name = "permission_id", nullable = false)
    private String permissionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}