package com.jira.project.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLevelMemberResponse {

    private UUID id;
    private UUID securityLevelId;
    private String memberType;
    private UUID memberId;
    private String groupName;
    private UUID addedBy;
    private LocalDateTime addedAt;
}