package com.avionics_systems.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private Boolean active;
    private Set<String> roles;
}
