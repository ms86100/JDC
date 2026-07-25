package com.jira.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSecurityLevelRequest {

    private UUID schemeId;

    @NotBlank(message = "{validation.security.name.required}")
    @Size(max = 100, message = "{validation.security.name.max}")
    private String name;

    private String description;

    @Size(max = 20, message = "{validation.security.leveltype.max}")
    private String levelType;

    private Integer sequence;
}