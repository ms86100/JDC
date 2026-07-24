package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "system_standard_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStandardMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "standard_type", length = 20)
    private String standardType;

    @Column(name = "spec_freeze_date")
    private LocalDate specFreezeDate;

    @Column(name = "delivery_to_lab_date")
    private LocalDate deliveryToLabDate;

    @Column(name = "requested_lab_clearance_date")
    private LocalDate requestedLabClearanceDate;

    @Column(name = "planned_flight_clearance_date")
    private LocalDate plannedFlightClearanceDate;

    @Column(name = "target_flight_date")
    private LocalDate targetFlightDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applicability", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicability = List.of();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "component_ids", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> componentIds = List.of();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
