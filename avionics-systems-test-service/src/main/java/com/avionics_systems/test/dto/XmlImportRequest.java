package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XmlImportRequest {
    @NotNull
    private UUID projectId;
    private String xmlContent;
    private UUID testSetId;
}
