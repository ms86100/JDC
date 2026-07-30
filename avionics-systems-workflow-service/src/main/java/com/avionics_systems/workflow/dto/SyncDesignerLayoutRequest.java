package com.avionics_systems.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SyncDesignerLayoutRequest {
    @NotEmpty
    @Valid
    private List<NodePosition> nodes;

    @Data
    public static class NodePosition {
        private UUID nodeId;
        private Double positionX;
        private Double positionY;
    }
}
