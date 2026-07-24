package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliverable_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverableMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "deliverable_type", length = 20)
    private String deliverableType;

    @Column(name = "milestone_type", length = 30)
    private String milestoneType;

    @Column(name = "baseline_start_date")
    private LocalDate baselineStartDate;

    @Column(name = "baseline_end_date")
    private LocalDate baselineEndDate;

    @Column(name = "external_end_date")
    private LocalDate externalEndDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "program_rebaselining", length = 10)
    private String programRebaselining;

    @Column(name = "source_of_delay", length = 50)
    private String sourceOfDelay;

    @Column(name = "risk_probability", length = 20)
    private String riskProbability;

    @Column(name = "risk_consequence", length = 20)
    private String riskConsequence;

    @Column(name = "risk_description", columnDefinition = "TEXT")
    private String riskDescription;

    @Column(name = "risk_owner")
    private UUID riskOwner;

    @Column(name = "risk_mitigation", length = 20)
    private String riskMitigation;

    @Column(name = "review_status", length = 20)
    private String reviewStatus;

    @Column(name = "review_assignee")
    private UUID reviewAssignee;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "review_start_date")
    private LocalDate reviewStartDate;

    @Column(name = "review_deadline")
    private LocalDate reviewDeadline;

    @Column(name = "domain_leader")
    private UUID domainLeader;

    @Column(name = "computer", length = 50)
    private String computer;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
