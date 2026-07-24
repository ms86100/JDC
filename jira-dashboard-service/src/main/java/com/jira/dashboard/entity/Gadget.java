package com.jira.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Entity
@Table(name = "gadgets", schema = "jira_dashboard",
    indexes = {
        @Index(name = "idx_gadget_category", columnList = "category"),
        @Index(name = "idx_gadget_module_key", columnList = "module_key")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gadget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "module_key", nullable = false, length = 255)
    private String moduleKey;

    @Column(nullable = false, length = 100)
    private String category; // activity, security, administration, etc.

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String configSchema; // JSON schema for gadget configuration

    @Column(columnDefinition = "TEXT")
    private String configDefaults; // JSON default configuration

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_sensitive", nullable = false)
    @Builder.Default
    private Boolean isSensitive = false;

    @Column(name = "permission_type", length = 100)
    @Builder.Default
    private String permissionType = "PROJECT"; // PROJECT, ADMIN, ALL

    @Column(name = "api_version", length = 20)
    @Builder.Default
    private String apiVersion = "1.0";
}