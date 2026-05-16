package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDto {
    private String id;
    private String username;
    private String displayName;
    private String email;
    private String userType;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}