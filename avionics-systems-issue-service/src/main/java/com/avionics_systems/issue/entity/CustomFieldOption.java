package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "custom_field_options", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(length = 500)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disabled = false;

    @Column(name = "parent_option_id")
    private UUID parentOptionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
