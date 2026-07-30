package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_plan_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_plan_id", nullable = false)
    private UUID testPlanId;

    @Column(name = "test_set_id", nullable = false)
    private UUID testSetId;

    @Column(name = "execution_order")
    private Integer executionOrder;

    @CreationTimestamp
    @Column(name = "added_at")
    private LocalDateTime addedAt;
}