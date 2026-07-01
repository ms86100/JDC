package com.jira.user.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String timezone;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}