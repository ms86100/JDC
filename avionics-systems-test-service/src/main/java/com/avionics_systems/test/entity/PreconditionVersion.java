package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "precondition_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreconditionVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "precondition_id", nullable = false)
    private UUID preconditionId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "condition_script", columnDefinition = "TEXT")
    private String conditionScript;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @Column(length = 50)
    private String category;

    @Column(name = "change_type", length = 50)
    private String changeType;

    @Column(name = "old_content", columnDefinition = "TEXT")
    private String oldContent;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}