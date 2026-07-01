package com.jira.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String userName;
    private String emailAddress;
    private String displayName;
    private String firstName;
    private String lastName;
    private boolean active;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private UUID directoryId;
    private String directoryName;
    private List<GroupInfo> groups;
    private List<String> applications;
    private LoginInfo loginInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupInfo {
        private UUID id;
        private String name;
        private boolean isAdmin;
        private boolean isJiraSoftware;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginInfo {
        private int loginCount;
        private LocalDateTime lastLogin;
    }
}