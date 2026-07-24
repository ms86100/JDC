package com.jira.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saml_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SamlConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "registration_id", nullable = false, unique = true, length = 100)
    private String registrationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "entity_id", nullable = false, length = 500)
    private String entityId;

    @Column(name = "idp_entity_id", nullable = false, length = 500)
    private String idpEntityId;

    @Column(name = "idp_sso_url", nullable = false, length = 1000)
    private String idpSsoUrl;

    @Column(name = "idp_slo_url", length = 1000)
    private String idpSloUrl;

    @Column(name = "idp_certificate", nullable = false, columnDefinition = "TEXT")
    private String idpCertificate;

    @Column(name = "sp_entity_id", length = 500)
    private String spEntityId;

    @Column(name = "acs_url", length = 1000)
    private String acsUrl;

    @Column(name = "attribute_mapping_email", length = 200)
    @Builder.Default
    private String attributeMappingEmail = "email";

    @Column(name = "attribute_mapping_username", length = 200)
    @Builder.Default
    private String attributeMappingUsername = "username";

    @Column(name = "attribute_mapping_display_name", length = 200)
    @Builder.Default
    private String attributeMappingDisplayName = "displayName";

    @Column(name = "attribute_mapping_groups", length = 200)
    @Builder.Default
    private String attributeMappingGroups = "groups";

    @Column(name = "default_role", length = 50)
    @Builder.Default
    private String defaultRole = "ROLE_USER";

    @Column(name = "auto_create_users", nullable = false)
    @Builder.Default
    private Boolean autoCreateUsers = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "force_authn", nullable = false)
    @Builder.Default
    private Boolean forceAuthn = false;

    @Column(name = "single_logout_enabled", nullable = false)
    @Builder.Default
    private Boolean singleLogoutEnabled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
