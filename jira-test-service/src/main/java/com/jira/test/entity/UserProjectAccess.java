package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_project_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProjectAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 50)
    private String role; // ADMIN, TEST_MANAGER, TESTER, VIEWER, DEVELOPER

    @Column
    @Builder.Default
    private Boolean hasCreatePermission = false;

    @Column
    @Builder.Default
    private Boolean hasUpdatePermission = false;

    @Column
    @Builder.Default
    private Boolean hasDeletePermission = false;

    @Column
    @Builder.Default
    private Boolean hasExecutePermission = false;

    @Column
    @Builder.Default
    private Boolean hasImportPermission = false;

    @Column
    @Builder.Default
    private Boolean hasReportPermission = false;

    @CreationTimestamp
    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;
}