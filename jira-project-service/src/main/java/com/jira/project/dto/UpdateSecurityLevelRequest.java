package com.jira.project.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSecurityLevelRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String description;

    @Size(max = 20, message = "Level type must not exceed 20 characters")
    private String levelType;

    private Integer sequence;
}