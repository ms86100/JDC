package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ldap_configurations")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LdapConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String ldapHost;

    private Integer ldapPort;

    @Column(name = "use_ssl")
    private Boolean useSsl = false;

    @Column(name = "base_dn")
    private String baseDn;

    @Column(name = "user_search_filter")
    private String userSearchFilter;

    @Column(name = "user_search_base")
    private String userSearchBase;

    @Column(name = "group_search_filter")
    private String groupSearchFilter;

    @Column(name = "group_search_base")
    private String groupSearchBase;

    private String managerDn;

    private String managerPassword;

    @Column(name = "auto_add_groups")
    private Boolean autoAddGroups = false;

    @Column(name = "sync_groups")
    private Boolean syncGroups = true;

    private Integer syncInterval;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "last_test_at")
    private LocalDateTime lastTestAt;
}