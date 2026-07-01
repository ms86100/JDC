package com.jira.migration.cluster;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing leader election across the cluster.
 * Provides leader election, heartbeat renewal, and leadership transfer capabilities.
 */
public interface LeaderElectionService {

    /**
     * Attempt to become leader for the given group.
     *
     * @param leadershipGroup The leadership group to lead
     * @return true if this node became leader, false otherwise
     */
    boolean tryBecomeLeader(String leadershipGroup);

    /**
     * Check if the current node is the leader for the given group.
     *
     * @param leadershipGroup The leadership group to check
     * @return true if this node is the leader
     */
    boolean isLeader(String leadershipGroup);

    /**
     * Get the current leader information for a group.
     *
     * @param leadershipGroup The leadership group
     * @return LeaderInfo if there is a current leader, empty otherwise
     */
    Optional<LeaderInfo> getLeader(String leadershipGroup);

    /**
     * Voluntarily resign leadership for a group.
     *
     * @param leadershipGroup The leadership group to resign from
     */
    void resign(String leadershipGroup);

    /**
     * Get all leadership groups managed by this service.
     *
     * @return List of leadership group names
     */
    List<String> getLeadershipGroups();

    /**
     * Add a listener for leader change events.
     *
     * @param leadershipGroup The leadership group to listen to
     * @param listener        The listener to add
     */
    void addLeaderChangeListener(String leadershipGroup, LeaderChangeListener listener);

    /**
     * Remove a listener for leader change events.
     *
     * @param leadershipGroup The leadership group
     * @param listener        The listener to remove
     */
    void removeLeaderChangeListener(String leadershipGroup, LeaderChangeListener listener);

    /**
     * Extend leadership lease (called periodically by leader).
     *
     * @param leadershipGroup The leadership group
     * @return true if lease was extended successfully
     */
    boolean extendLeadership(String leadershipGroup);

    /**
     * Get the term number for a leadership group.
     *
     * @param leadershipGroup The leadership group
     * @return Current term number, or 0 if not leader
     */
    long getTerm(String leadershipGroup);

    /**
     * Check if leadership is active and not expired.
     *
     * @param leadershipGroup The leadership group
     * @return true if there is a valid leader
     */
    boolean hasActiveLeader(String leadershipGroup);

    /**
     * Get all current leaders.
     *
     * @return List of all active leader info
     */
    List<LeaderInfo> getAllLeaders();

    /**
     * Delete a leadership group and all its state.
     *
     * @param leadershipGroup The leadership group to delete
     */
    void deleteLeadershipGroup(String leadershipGroup);
}