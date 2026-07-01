package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "defect_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "defect_key", nullable = false, length = 100)
    private String defectKey;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "step_result_id")
    private UUID stepResultId;

    @Column(length = 20)
    private String severity;

    @Column(length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}