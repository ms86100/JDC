package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "precondition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Precondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "precondition_type", length = 50)
    @Builder.Default
    private String preconditionType = "AUTOMATED";

    @Column(name = "condition_script", columnDefinition = "TEXT")
    private String conditionScript;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "category", length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column
    @Builder.Default
    private Integer version = 1;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}