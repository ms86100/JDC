package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_precondition_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestPreconditionLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "precondition_id", nullable = false)
    private UUID preconditionId;

    @Column(name = "step_order")
    @Builder.Default
    private Integer stepOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "depends_on_preconditions", columnDefinition = "TEXT")
    private String dependsOnPreconditions;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "ignore_failure")
    @Builder.Default
    private Boolean ignoreFailure = false;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}