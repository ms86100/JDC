package com.jira.plan.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {

    private UUID id;
    private UUID teamId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String userAvatarUrl;
    private BigDecimal capacityHours;
    private BigDecimal allocatedHours;
    private String role;
    private LocalDateTime joinedAt;
}
