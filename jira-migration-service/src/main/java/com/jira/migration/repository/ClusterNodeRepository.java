package com.jira.migration.repository;

import com.jira.migration.entity.ClusterNodeEntity;
import com.jira.migration.entity.NodeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for cluster node operations.
 */
@Repository
public interface ClusterNodeRepository extends JpaRepository<ClusterNodeEntity, String> {

    /**
     * Find all nodes in a specific state.
     */
    List<ClusterNodeEntity> findByState(NodeState state);

    /**
     * Find all active nodes.
     */
    @Query("SELECT c FROM ClusterNodeEntity c WHERE c.state = 'ACTIVE'")
    List<ClusterNodeEntity> findAllActive();

    /**
     * Find nodes with heartbeat after a threshold.
     */
    @Query("SELECT c FROM ClusterNodeEntity c WHERE c.lastHeartbeat > :threshold AND c.state != 'TERMINATED'")
    List<ClusterNodeEntity> findActiveNodesWithRecentHeartbeat(@Param("threshold") Instant threshold);

    /**
     * Update heartbeat for a node.
     */
    @Modifying
    @Query("UPDATE ClusterNodeEntity c SET c.lastHeartbeat = :now, c.version = c.version + 1 " +
           "WHERE c.nodeId = :nodeId")
    int updateHeartbeat(@Param("nodeId") String nodeId, @Param("now") Instant now);

    /**
     * Update node state.
     */
    @Modifying
    @Query("UPDATE ClusterNodeEntity c SET c.state = :state, c.lastHeartbeat = :now " +
           "WHERE c.nodeId = :nodeId")
    int updateState(@Param("nodeId") String nodeId, @Param("state") NodeState state, @Param("now") Instant now);

    /**
     * Clean up stale nodes (no heartbeat since threshold).
     */
    @Modifying
    @Query("DELETE FROM ClusterNodeEntity c WHERE c.lastHeartbeat < :threshold AND c.state != 'TERMINATED'")
    int cleanupStaleNodes(@Param("threshold") Instant threshold);

    /**
     * Find nodes that haven't heartbeat since threshold.
     */
    @Query("SELECT c FROM ClusterNodeEntity c WHERE c.lastHeartbeat < :threshold AND c.state = 'ACTIVE'")
    List<ClusterNodeEntity> findStaleActiveNodes(@Param("threshold") Instant threshold);

    /**
     * Count active nodes.
     */
    @Query("SELECT COUNT(c) FROM ClusterNodeEntity c WHERE c.state = 'ACTIVE'")
    long countActiveNodes();

    /**
     * Count all non-terminated nodes.
     */
    @Query("SELECT COUNT(c) FROM ClusterNodeEntity c WHERE c.state != 'TERMINATED'")
    long countAllActiveNodes();

    /**
     * Find node by host and port.
     */
    Optional<ClusterNodeEntity> findByHostAndPort(String host, Integer port);

    /**
     * Update job count for a node.
     */
    @Modifying
    @Query("UPDATE ClusterNodeEntity c SET c.currentJobs = :jobCount WHERE c.nodeId = :nodeId")
    int updateJobCount(@Param("nodeId") String nodeId, @Param("jobCount") Integer jobCount);

    /**
     * Increment job count.
     */
    @Modifying
    @Query("UPDATE ClusterNodeEntity c SET c.currentJobs = c.currentJobs + 1 WHERE c.nodeId = :nodeId")
    int incrementJobCount(@Param("nodeId") String nodeId);

    /**
     * Decrement job count.
     */
    @Modifying
    @Query("UPDATE ClusterNodeEntity c SET c.currentJobs = CASE WHEN c.currentJobs > 0 THEN c.currentJobs - 1 ELSE 0 END " +
           "WHERE c.nodeId = :nodeId")
    int decrementJobCount(@Param("nodeId") String nodeId);

    /**
     * Find nodes that can accept jobs.
     */
    @Query("SELECT c FROM ClusterNodeEntity c WHERE c.state = 'ACTIVE' AND c.currentJobs < c.maxJobs")
    List<ClusterNodeEntity> findNodesThatCanAcceptJobs();
}