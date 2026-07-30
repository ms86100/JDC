package com.avionics_systems.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_group_memberships", schema = "jira_auth")
@IdClass(UserGroupMembershipId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserGroupMembership implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column(name = "added_by")
    private UUID addedBy;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}