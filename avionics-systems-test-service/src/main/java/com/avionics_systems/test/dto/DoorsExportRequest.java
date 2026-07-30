package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoorsExportRequest {

    @NotNull
    private UUID projectId;

    @NotNull
    private UUID fixVersionId;
}
