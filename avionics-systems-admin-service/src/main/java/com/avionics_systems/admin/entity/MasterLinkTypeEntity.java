package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "master_link_types", schema = "jira_admin")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MasterLinkTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "link_key", nullable = false, unique = true, length = 50)
    private String linkKey;

    @Column(name = "outward_name", nullable = false, length = 100)
    private String outwardName;

    @Column(name = "inward_name", nullable = false, length = 100)
    private String inwardName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
