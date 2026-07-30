package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LexoRankResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private Long bucketId;
    private String rankValue;
    private Boolean locked;
    private LocalDateTime lockedAt;
    private UUID lockedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}