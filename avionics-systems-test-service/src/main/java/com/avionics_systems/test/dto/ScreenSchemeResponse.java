package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSchemeResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private Boolean isDefault;
    private List<ScreenSchemeScreenResponse> screens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}