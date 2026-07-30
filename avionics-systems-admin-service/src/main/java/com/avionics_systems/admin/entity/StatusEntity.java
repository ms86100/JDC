package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "statuses")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status_category")
    private String statusCategory;

    private Integer sequence;

    @Column(name = "status_color")
    private String statusColor;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "lookup_group")
    private String lookupGroup;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Status categories (Avionics Systems standard)
    public static final String CATEGORY_TODO = "TODO";
    public static final String CATEGORY_IN_PROGRESS = "IN_PROGRESS";
    public static final String CATEGORY_DONE = "DONE";
}