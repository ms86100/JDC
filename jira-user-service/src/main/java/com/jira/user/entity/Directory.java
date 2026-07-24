package com.jira.user.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "directories", schema = "jira_admin")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "directory_name", nullable = false)
    private String directoryName;

    @Column(name = "directory_type", nullable = false)
    private String directoryType;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "order_index")
    private int orderIndex;

    @Column(name = "server_url", length = 500)
    private String serverUrl;

    @Column(name = "base_dn", length = 500)
    private String baseDn;

    @Column(name = "bind_dn", length = 500)
    private String bindDn;

    @Column(name = "encrypted_bind_password")
    private String encryptedBindPassword;

    @Column(name = "user_search_base", length = 500)
    private String userSearchBase;

    @Column(name = "user_search_filter", length = 500)
    private String userSearchFilter;

    @Column(name = "group_search_base", length = 500)
    private String groupSearchBase;

    @Column(name = "group_search_filter", length = 500)
    private String groupSearchFilter;

    @Column(name = "sync_interval_minutes")
    private Integer syncIntervalMinutes;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "sync_status", length = 20)
    private String syncStatus;
}