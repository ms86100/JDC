package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_quick_filter_sharing", schema = "jira_plan")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BoardQuickFilterSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quick_filter_id", nullable = false)
    private UUID quickFilterId;

    @Column(name = "shared_with_user_id")
    private UUID sharedWithUserId;

    @Column(name = "shared_with_group")
    private String sharedWithGroup;

    @Column(name = "permission_level", length = 20)
    @Builder.Default
    private String permissionLevel = "VIEW";

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;

    @Column(name = "shared_by")
    private UUID sharedBy;
}