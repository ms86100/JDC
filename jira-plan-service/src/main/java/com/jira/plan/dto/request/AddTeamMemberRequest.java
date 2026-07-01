package com.jira.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTeamMemberRequest {

    @NotBlank(message = "User ID is required")
    private UUID userId;

    private String userName;

    private BigDecimal capacityHours;

    private String role;
}