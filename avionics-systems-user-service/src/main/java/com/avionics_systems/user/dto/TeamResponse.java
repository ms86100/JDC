package com.avionics_systems.user.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
}