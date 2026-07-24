package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Non-working day (holiday/vacation).
 */
@Entity
@Table(name = "non_working_days", schema = "jira_plan", indexes = {
    @Index(name = "idx_non_working_days_config", columnList = "config_id"),
    @Index(name = "idx_non_working_days_date", columnList = "date")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonWorkingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private WorkingDays workingDays;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String name;  // e.g., "Christmas", "Company Holiday"

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}