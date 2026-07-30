package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VvoTransferResponse {

    private UUID sourceProjectId;
    private UUID targetProjectId;
    private UUID fixVersionId;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private List<String> details;
    private boolean previewOnly;
}
