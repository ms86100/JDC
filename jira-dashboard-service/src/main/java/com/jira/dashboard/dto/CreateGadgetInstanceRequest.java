package com.jira.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGadgetInstanceRequest {

    @NotNull(message = "Gadget ID is required")
    private UUID gadgetId;

    private String title;

    @Builder.Default
    private Integer positionRow = 0;

    @Builder.Default
    private Integer positionColumn = 0;

    @Builder.Default
    private Integer width = 1;

    @Builder.Default
    private Integer height = 1;

    private String config;

    private String filters;

    private String color = "#ffffff";
}