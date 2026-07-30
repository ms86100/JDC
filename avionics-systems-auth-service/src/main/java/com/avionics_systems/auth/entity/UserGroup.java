package com.avionics_systems.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_groups", schema = "jira_auth")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_name", nullable = false, unique = true, length = 100)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_type", length = 50)
    @Builder.Default
    private String groupType = "AVIONICS_SYSTEMS_INTERNAL";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserGroupMembership> memberships = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}