package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkTestRequest {
    @NotNull
    private List<UUID> testIds;
    private String status;
    private UUID ownerId;
    private UUID folderId;
    private UUID testSetId;
    private List<String> labels;
}
