package com.avionics_systems.user.entity;

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
@Table(name = "cwd_user", schema = "jira_admin")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CwdUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "directory_id", nullable = false)
    private UUID directoryId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "lower_user_name", nullable = false)
    private String lowerUserName;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "failed_auth_count")
    private int failedAuthCount;

    @Column(name = "last_auth_date")
    private LocalDateTime lastAuthDate;

    @Column(name = "credential_expire_date")
    private LocalDateTime credentialExpireDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (updatedDate == null) updatedDate = LocalDateTime.now();
        if (lowerUserName == null && userName != null) {
            lowerUserName = userName.toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        if (lowerUserName == null && userName != null) {
            lowerUserName = userName.toLowerCase();
        }
    }
}