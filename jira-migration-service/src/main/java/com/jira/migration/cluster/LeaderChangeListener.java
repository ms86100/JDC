package com.jira.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Listener interface for leader change events.
 */
@FunctionalInterface
public interface LeaderChangeListener {

    /**
     * Called when the leader changes for a group.
     *
     * @param group     The leadership group
     * @param oldLeader The previous leader (can be null if this is the first leader)
     * @param newLeader The new leader info
     */
    void onLeaderChanged(String group, LeaderInfo oldLeader, LeaderInfo newLeader);
}