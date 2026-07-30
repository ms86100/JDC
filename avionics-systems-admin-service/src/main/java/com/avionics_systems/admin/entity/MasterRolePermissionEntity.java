package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "master_role_permissions", schema = "jira_admin",
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MasterRolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private MasterRoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private MasterPermissionEntity permission;
}
