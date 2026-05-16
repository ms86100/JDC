package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_permissions", schema = "jira_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "permission_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PermissionType permissionType;

    @Column(name = "principal_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    public enum PermissionType {
        VIEW,
        EDIT,
        ADMIN,
        MANAGE_MEMBERS,
        MANAGE_SETTINGS,
        VIEW_REPORTS,
        EXPORT
    }

    public enum PrincipalType {
        USER,
        GROUP,
        PROJECT_ROLE
    }

    public String getPermissionKey() {
        return permissionType.name() + "_" + principalType.name() + "_" + principalId;
    }
}