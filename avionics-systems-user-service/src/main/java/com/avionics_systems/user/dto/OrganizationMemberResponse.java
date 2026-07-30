package com.avionics_systems.user.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {

    private UUID orgId;
    private UUID userId;
    private String role;
    private String userFirstName;
    private String userLastName;
    private OffsetDateTime joinedAt;
}