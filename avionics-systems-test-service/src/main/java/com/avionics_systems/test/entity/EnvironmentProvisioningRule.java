package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "environment_provisioning_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentProvisioningRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType; // BROWSERSTACK, SAUCELABS, KUBERNETES, DOCKER, LOCAL

    @Column(name = "provider_config", columnDefinition = "JSONB", nullable = false)
    private String providerConfig; // {apiKey: "...", project: "..."}

    @Column(name = "provisioning_script", columnDefinition = "TEXT")
    private String provisioningScript; // Custom provisioning script

    @Column(name = "capabilities_template", columnDefinition = "JSONB")
    private String capabilitiesTemplate; // WebDriver capabilities template

    @Column(name = "environment_template", columnDefinition = "JSONB")
    private String environmentTemplate; // Template for env vars

    @Column(name = "max_concurrent")
    @Builder.Default
    private Integer maxConcurrent = 5;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 300;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 3;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // Higher = more priority

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}