package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "precondition_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreconditionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String category;

    @Column(name = "precondition_type", length = 50)
    @Builder.Default
    private String preconditionType = "AUTOMATED";

    @Column(name = "condition_script", columnDefinition = "TEXT")
    private String conditionScript;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "is_system_template")
    @Builder.Default
    private Boolean isSystemTemplate = false;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}