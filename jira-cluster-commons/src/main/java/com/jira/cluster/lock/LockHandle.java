package com.jira.cluster.lock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockHandle {

    private String resource;
    private String lockId;
    private String ownerId;
    private String nodeId;
    private Instant acquiredAt;
    private Instant expiresAt;
    private String lockType;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public long getRemainingTtlSeconds() {
        if (expiresAt == null) {
            return 0;
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}
