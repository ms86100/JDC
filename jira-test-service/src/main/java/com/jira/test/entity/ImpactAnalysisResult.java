package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "impact_analysis_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType; // commit, pr, manual, schedule

    @Column(name = "trigger_id")
    private UUID triggerId;

    @Column(name = "analysis_payload", columnDefinition = "JSONB", nullable = false)
    private String analysisPayload;

    @Column(name = "suggested_suite", columnDefinition = "JSONB")
    private String suggestedSuite;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "analyzed_by", length = 100)
    @Builder.Default
    private String analyzedBy = "rule-based";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}