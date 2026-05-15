package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a User in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto {

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
    private String timeZone;
    private String locale;
    private List<String> groups;
    private List<String> roles;
    private List<String> projectPermissions;
    private String timezone;
    private String phone;
    private String mobile;
    private String skype;
}