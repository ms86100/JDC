package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_ip")
    private String userIp;

    @Column(nullable = false)
    private String action;

    private String category;

    @Column(nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "changed_values", columnDefinition = "TEXT")
    private String changedValues;

    @Column(columnDefinition = "TEXT")
    private String details;

    private String result;

    private String severity;

    private String source;

    @Column(name = "user_agent")
    private String userAgent;
}