package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEntity {

    public enum ServiceStatus {
        HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String serviceId;

    @Column(nullable = false)
    private String serviceName;

    private String description;

    @Column(nullable = false)
    private String serviceType;

    @Enumerated(EnumType.STRING)
    private ServiceStatus serviceStatus = ServiceStatus.UNKNOWN;

    private String url;

    private String version;

    @Column(name = "is_running")
    private Boolean isRunning = false;

    @Column(name = "last_started_at")
    private LocalDateTime lastStartedAt;

    @Column(name = "last_check")
    private LocalDateTime lastCheck;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}