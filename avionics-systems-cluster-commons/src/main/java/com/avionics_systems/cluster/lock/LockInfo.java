package com.avionics_systems.cluster.lock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockInfo {

    private String resource;
    private String lockId;
    private String ownerId;
    private String nodeId;
    private Instant acquiredAt;
    private Instant expiresAt;
    private boolean isHeld;
    private String lockType;
    private long remainingTtlSeconds;

    public static LockInfo fromHandle(LockHandle handle) {
        return LockInfo.builder()
                .resource(handle.getResource())
                .lockId(handle.getLockId())
                .ownerId(handle.getOwnerId())
                .nodeId(handle.getNodeId())
                .acquiredAt(handle.getAcquiredAt())
                .expiresAt(handle.getExpiresAt())
                .isHeld(true)
                .lockType(handle.getLockType())
                .remainingTtlSeconds(handle.getRemainingTtlSeconds())
                .build();
    }
}
