package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_step")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type", nullable = false, length = 20)
    private String stepType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "test_data", columnDefinition = "TEXT")
    private String testData;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}