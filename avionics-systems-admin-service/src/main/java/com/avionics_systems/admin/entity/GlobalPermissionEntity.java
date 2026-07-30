package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_permissions")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String permissionKey;

    private String permissionName;

    private String permissionType;

    @Column(name = "granted_to_type")
    private String grantedToType;

    @Column(name = "granted_to_id")
    private String grantedToId;

    private String grantedBy;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "user_id")
    private String userId;
}