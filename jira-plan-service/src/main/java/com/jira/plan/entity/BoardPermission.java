package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Board permission for access control.
 */
@Entity
@Table(name = "board_permissions", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_permissions_board", columnList = "board_id"),
    @Index(name = "idx_board_permissions_principal", columnList = "principal_type, principal_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardConfig boardConfig;

    @Column(name = "permission_type", nullable = false, length = 50)
    private String permissionType;  // VIEW, EDIT, ADMIN, MANAGE_SPRINTS, EDIT_SPRINTS

    @Column(name = "principal_type", nullable = false, length = 20)
    private String principalType;  // USER, GROUP

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;
}