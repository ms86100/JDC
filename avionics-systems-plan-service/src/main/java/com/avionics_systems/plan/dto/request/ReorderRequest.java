package com.avionics_systems.plan.dto.request;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {

    private UUID itemId;
    private String newSortOrder;
    private UUID newParentId;
    private UUID afterItemId;
    private UUID beforeItemId;
}