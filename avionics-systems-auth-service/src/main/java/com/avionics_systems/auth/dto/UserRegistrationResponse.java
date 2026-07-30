package com.avionics_systems.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserRegistrationResponse {
    private UUID userId;
    private String username;
    private String email;
    private Set<String> roles;
    private Set<String> groups;
    private LocalDateTime createdAt;
    private String message;
}