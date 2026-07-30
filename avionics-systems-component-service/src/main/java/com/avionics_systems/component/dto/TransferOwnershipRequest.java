package com.avionics_systems.component.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferOwnershipRequest {
    private UUID newLeadId;
    private String reason;
    private UUID transferredBy;
}