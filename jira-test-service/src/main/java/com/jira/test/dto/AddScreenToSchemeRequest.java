package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddScreenToSchemeRequest {

    @NotNull(message = "Screen ID is required")
    private UUID screenId;

    @NotNull(message = "Screen type is required")
    private String screenType;
}