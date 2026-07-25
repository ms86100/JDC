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
public class CreateProfileRequest {

    @NotNull(message = "{validation.userid.required}")
    private UUID userId;

    @NotBlank(message = "{validation.firstname.required}")
    @Size(max = 100, message = "{validation.firstname.size}")
    private String firstName;

    @NotBlank(message = "{validation.lastname.required}")
    @Size(max = 100, message = "{validation.lastname.size}")
    private String lastName;

    @Size(max = 500, message = "{validation.avatar.size}")
    private String avatarUrl;

    @Size(max = 50, message = "{validation.timezone.size}")
    private String timezone;
}