package com.jira.project.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @JsonAlias("key")
    @Size(min = 2, max = 10, message = "{validation.project.key.size}")
    @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "{validation.project.key.format}")
    private String projectKey;

    @NotBlank(message = "{validation.project.name.required}")
    @Size(max = 200, message = "{validation.project.name.max}")
    private String name;

    @Size(max = 1000, message = "{validation.project.description.max}")
    private String description;

    private UUID leadUserId;
}