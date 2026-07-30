package com.avionics_systems.component.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssignComponentRequest {
    private List<UUID> issueIds;
    private UUID componentId;
    private Boolean remove; // if true, remove from components
}