package com.avionics_systems.component.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipTransferResponse {
    private UUID id;
    private UUID componentId;
    private UUID previousLeadId;
    private UUID newLeadId;
    private String transferReason;
    private UUID transferredBy;
    private LocalDateTime transferredAt;
}