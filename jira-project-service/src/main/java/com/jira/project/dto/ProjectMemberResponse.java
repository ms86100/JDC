package com.jira.project.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private UUID projectId;
    private UUID userId;
    private UUID projectRoleId;
    private String roleName;
    private List<String> permissions;
    private LocalDateTime joinedAt;
}