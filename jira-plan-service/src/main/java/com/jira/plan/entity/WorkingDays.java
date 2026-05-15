package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Working days configuration (calendar template).
 * Used for capacity planning and sprint date calculations.
 */
@Entity
@Table(name = "working_days_config", schema = "jira_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingDays {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "monday")
    @Builder.Default
    private Boolean monday = true;

    @Column(name = "tuesday")
    @Builder.Default
    private Boolean tuesday = true;

    @Column(name = "wednesday")
    @Builder.Default
    private Boolean wednesday = true;

    @Column(name = "thursday")
    @Builder.Default
    private Boolean thursday = true;

    @Column(name = "friday")
    @Builder.Default
    private Boolean friday = true;

    @Column(name = "saturday")
    @Builder.Default
    private Boolean saturday = false;

    @Column(name = "sunday")
    @Builder.Default
    private Boolean sunday = false;

    @Column(name = "hours_per_day", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal hoursPerDay = new BigDecimal("8.00");

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "workingDays", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NonWorkingDay> nonWorkingDays = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if a given date is a working day.
     */
    public boolean isWorkingDay(LocalDate date) {
        java.time.DayOfWeek day = date.getDayOfWeek();
        switch (day) {
            case MONDAY:    return Boolean.TRUE.equals(monday);
            case TUESDAY:   return Boolean.TRUE.equals(tuesday);
            case WEDNESDAY: return Boolean.TRUE.equals(wednesday);
            case THURSDAY:  return Boolean.TRUE.equals(thursday);
            case FRIDAY:    return Boolean.TRUE.equals(friday);
            case SATURDAY:  return Boolean.TRUE.equals(saturday);
            case SUNDAY:    return Boolean.TRUE.equals(sunday);
            default:        return false;
        }
    }

    /**
     * Check if a day of week is a working day.
     */
    public boolean isWorkingDay(java.time.DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:    return Boolean.TRUE.equals(monday);
            case TUESDAY:   return Boolean.TRUE.equals(tuesday);
            case WEDNESDAY: return Boolean.TRUE.equals(wednesday);
            case THURSDAY:  return Boolean.TRUE.equals(thursday);
            case FRIDAY:    return Boolean.TRUE.equals(friday);
            case SATURDAY:  return Boolean.TRUE.equals(saturday);
            case SUNDAY:    return Boolean.TRUE.equals(sunday);
            default:        return false;
        }
    }

    /**
     * Count working days in a week (excluding holidays).
     */
    public int getWorkingDaysPerWeek() {
        int count = 0;
        if (Boolean.TRUE.equals(monday)) count++;
        if (Boolean.TRUE.equals(tuesday)) count++;
        if (Boolean.TRUE.equals(wednesday)) count++;
        if (Boolean.TRUE.equals(thursday)) count++;
        if (Boolean.TRUE.equals(friday)) count++;
        if (Boolean.TRUE.equals(saturday)) count++;
        if (Boolean.TRUE.equals(sunday)) count++;
        return count;
    }
}