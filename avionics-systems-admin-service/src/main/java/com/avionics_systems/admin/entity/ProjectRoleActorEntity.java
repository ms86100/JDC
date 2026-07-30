package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_role_actors")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoleActorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "project_role_id", nullable = false)
    private String projectRoleId;

    @Column(name = "holder_type", nullable = false)
    private String holderType;  // 'USER', 'GROUP'

    @Column(name = "holder_id", nullable = false)
    private String holderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}