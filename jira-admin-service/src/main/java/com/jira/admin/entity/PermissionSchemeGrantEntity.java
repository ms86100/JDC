package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "permission_scheme_grants")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSchemeGrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "permission_scheme_id", nullable = false)
    private String permissionSchemeId;

    @Column(name = "permission_id", nullable = false)
    private String permissionId;

    @Column(name = "holder_type", nullable = false)
    private String holderType;  // 'USER', 'GROUP', 'PROJECT_ROLE'

    @Column(name = "holder_id")
    private String holderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}