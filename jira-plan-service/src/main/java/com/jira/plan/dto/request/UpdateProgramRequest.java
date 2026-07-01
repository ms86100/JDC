package com.jira.plan.dto.request;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgramRequest {

    private String name;
    private String description;
    private String accessType;
    private Boolean isActive;
}