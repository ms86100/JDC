package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.LeaderElection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for leader election operations.
 */
@Repository
public interface LeaderElectionRepository extends JpaRepository<LeaderElection, String> {

    /**
     * Find election by leadership group.
     */
    Optional<LeaderElection> findByLeadershipGroup(String leadershipGroup);

    /**
     * Find all active (non-expired) leader elections.
     */
    @Query("SELECT l FROM LeaderElection l WHERE l.leaseExpiresAt > :now")
    List<LeaderElection> findAllActiveElections(@Param("now") Instant now);

    /**
     * Find all expired leader elections.
     */
    @Query("SELECT l FROM LeaderElection l WHERE l.leaseExpiresAt < :now")
    List<LeaderElection> findAllExpiredElections(@Param("now") Instant now);

    /**
     * Delete expired elections.
     */
    @Modifying
    @Query("DELETE FROM LeaderElection l WHERE l.leaseExpiresAt < :now")
    int deleteExpiredElections(@Param("now") Instant now);

    /**
     * Find current leader for a group (if not expired).
     */
    @Query("SELECT l FROM LeaderElection l WHERE l.leadershipGroup = :group AND l.leaseExpiresAt > :now")
    Optional<LeaderElection> findCurrentLeader(@Param("group") String group, @Param("now") Instant now);

    /**
     * Check if a specific node is the leader for a group.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LeaderElection l " +
           "WHERE l.leadershipGroup = :group AND l.nodeId = :nodeId AND l.leaseExpiresAt > :now")
    boolean isLeader(@Param("group") String group, @Param("nodeId") String nodeId, @Param("now") Instant now);

    /**
     * Update leader heartbeat and lease.
     */
    @Modifying
    @Query("UPDATE LeaderElection l SET l.lastHeartbeat = :heartbeat, l.leaseExpiresAt = :leaseExpiry, " +
           "l.term = l.term + 1 WHERE l.leadershipGroup = :group AND l.nodeId = :nodeId")
    int renewLease(@Param("group") String group,
                   @Param("nodeId") String nodeId,
                   @Param("heartbeat") Instant heartbeat,
                   @Param("leaseExpiry") Instant leaseExpiry);

    /**
     * Delete leader election by group.
     */
    @Modifying
    @Query("DELETE FROM LeaderElection l WHERE l.leadershipGroup = :group")
    int deleteByGroup(@Param("group") String group);

    /**
     * Delete all elections for a node.
     */
    @Modifying
    @Query("DELETE FROM LeaderElection l WHERE l.nodeId = :nodeId")
    int deleteByNodeId(@Param("nodeId") String nodeId);

    /**
     * Count active leaders.
     */
    @Query("SELECT COUNT(l) FROM LeaderElection l WHERE l.leaseExpiresAt > :now")
    long countActiveLeaders(@Param("now") Instant now);

    /**
     * Find all leadership groups.
     */
    @Query("SELECT DISTINCT l.leadershipGroup FROM LeaderElection l")
    List<String> findAllLeadershipGroups();
}