package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for User operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserResponse {

    @EqualsAndHashCode.Include
    private String id;

    private String username;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String userType;
    private boolean active;
    private boolean deleted;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime lastLogin;
    private String department;
    private String organization;
    private String jobTitle;
    private String timezone;
    private String locale;
    private boolean success;
    private String errorMessage;
}