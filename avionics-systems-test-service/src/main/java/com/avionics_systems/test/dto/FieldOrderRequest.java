package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldOrderRequest {

    @NotNull(message = "Field order list is required")
    private List<UUID> fieldOrder;
}