package com.jira.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {

    @NotBlank(message = "{validation.organization.name.required}")
    @Size(max = 255, message = "{validation.organization.name.size}")
    private String name;

    @NotBlank(message = "{validation.slug.required}")
    @Size(max = 255, message = "{validation.slug.size}")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "{validation.slug.pattern}")
    private String slug;
}