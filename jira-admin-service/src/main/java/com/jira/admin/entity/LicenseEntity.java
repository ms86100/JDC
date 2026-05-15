package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "license")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "license_type")
    private String licenseType = "Standard";

    @Column(name = "max_users")
    private Integer maxUsers = 100;

    @Column(name = "max_projects")
    private Integer maxProjects = 50;

    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "support_entitlement")
    private String supportEntitlement;

    @Column(name = "license_key", columnDefinition = "TEXT")
    private String licenseKey;

    @Column(name = "organisation_name")
    private String organisationName;
}