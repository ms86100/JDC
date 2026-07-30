package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenFieldRequest {

    @NotNull(message = "Field ID is required")
    private UUID fieldId;

    private Integer position;

    private Boolean isRequired = false;

    private Boolean isEditable = true;

    private Boolean isVisible = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScreenFieldsUpdateRequest {
        private List<ScreenFieldRequest> fields;
    }
}