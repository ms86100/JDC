package com.avionics_systems.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "filter_subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FilterSubscription {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "filter_id", nullable = false) private UUID filterId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 20) @Builder.Default private String frequency = "DAILY";
    @Column(name = "is_enabled", nullable = false) @Builder.Default private Boolean isEnabled = true;
    @Column(name = "last_run_at") private LocalDateTime lastRunAt;
    @Column(name = "next_run_at") private LocalDateTime nextRunAt;
    @Column(name = "email_address", length = 300) private String emailAddress;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
