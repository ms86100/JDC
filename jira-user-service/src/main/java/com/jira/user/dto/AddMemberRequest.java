package com.jira.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {

    @NotNull(message = "{validation.userid.required}")
    private UUID userId;

    @NotBlank(message = "{validation.role.required}")
    @Size(max = 50, message = "{validation.role.size}")
    private String role;
}