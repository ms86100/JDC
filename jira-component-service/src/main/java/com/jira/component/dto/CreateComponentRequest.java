package com.jira.component.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateComponentRequest {

    @NotNull(message = "{validation.projectId.required}")
    private UUID projectId;

    @NotBlank(message = "{validation.component.name.required}")
    private String name;

    private String description;

    private UUID leadUserId;

    private String assigneeType;

    private UUID defaultAssignee;

    private String color;

    private String icon;
}