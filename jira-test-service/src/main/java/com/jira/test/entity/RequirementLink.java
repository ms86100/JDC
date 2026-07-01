package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "requirement_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requirement_key", nullable = false)
    private String requirementKey;

    @Column(name = "requirement_type", length = 50)
    private String requirementType;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "coverage_status", length = 20)
    @Builder.Default
    private String coverageStatus = "COVERED";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}