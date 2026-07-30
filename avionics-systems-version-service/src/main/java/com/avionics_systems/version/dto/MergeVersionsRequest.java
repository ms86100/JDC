package com.avionics_systems.version.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeVersionsRequest {
    private UUID sourceVersionId;
    private UUID targetVersionId;
    private List<UUID> issueIdsToMove;
}