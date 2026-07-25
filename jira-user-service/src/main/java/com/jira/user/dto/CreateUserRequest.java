package com.jira.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.format}")
    private String email;

    @NotBlank(message = "{validation.fullname.required}")
    private String fullName;

    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 100, message = "{validation.username.size}")
    private String userName;

    private String password;

    @Builder.Default
    private boolean sendNotification = true;
}