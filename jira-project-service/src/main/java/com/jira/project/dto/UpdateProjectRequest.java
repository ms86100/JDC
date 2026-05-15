package com.jira.project.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    private String name;
    private String description;
    private UUID leadUserId;
}