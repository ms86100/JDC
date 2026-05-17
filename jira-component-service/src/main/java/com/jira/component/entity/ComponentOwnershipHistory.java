package com.jira.component.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "component_ownership_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentOwnershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "previous_lead_id")
    private UUID previousLeadId;

    @Column(name = "new_lead_id")
    private UUID newLeadId;

    @Column(name = "transfer_reason", columnDefinition = "TEXT")
    private String transferReason;

    @Column(name = "transferred_by")
    private UUID transferredBy;

    @Column(name = "transferred_at", nullable = false, updatable = false)
    private LocalDateTime transferredAt;

    @PrePersist
    protected void onCreate() {
        transferredAt = LocalDateTime.now();
    }
}