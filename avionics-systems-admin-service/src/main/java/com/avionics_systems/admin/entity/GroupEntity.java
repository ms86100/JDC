package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String groupType;

    @Transient
    @Builder.Default
    private List<UserEntity> users = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}