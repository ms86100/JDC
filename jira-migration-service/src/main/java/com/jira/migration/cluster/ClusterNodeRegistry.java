package com.jira.migration.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.config.ClusterConfig;
import com.jira.migration.entity.ClusterNodeEntity;
import com.jira.migration.entity.NodeState;
import com.jira.migration.repository.ClusterNodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for cluster nodes.
 * Manages node registration, heartbeat, and node discovery.
 */
@Slf4j
@Service
public class ClusterNodeRegistry {

    private final ClusterNodeRepository clusterNodeRepository;
    private final ClusterConfig clusterConfig;
    private final ObjectMapper objectMapper;

    // Local cache of cluster nodes
    private final Map<String, ClusterNode> nodeCache = new ConcurrentHashMap<>();

    // This node's info
    private ClusterNode thisNode;

    @Autowired
    public ClusterNodeRegistry(ClusterNodeRepository clusterNodeRepository, ClusterConfig clusterConfig) {
        this.clusterNodeRepository = clusterNodeRepository;
        this.clusterConfig = clusterConfig;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initialize() {
        if (!clusterConfig.isEnabled()) {
            log.info("Cluster coordination is disabled, skipping node registration");
            return;
        }

        // Generate node ID if not set
        if (clusterConfig.getNodeId() == null || clusterConfig.getNodeId().isEmpty()) {
            String nodeId = generateNodeId();
            clusterConfig.setNodeId(nodeId);
            log.info("Generated node ID: {}", nodeId);
        }

        // Set host if not configured
        if (clusterConfig.getHost() == null || clusterConfig.getHost().isEmpty()) {
            try {
                String host = InetAddress.getLocalHost().getHostName();
                clusterConfig.setHost(host);
            } catch (Exception e) {
                clusterConfig.setHost("unknown");
            }
        }

        // Initialize this node
        thisNode = ClusterNode.builder()
                .nodeId(clusterConfig.getNodeId())
                .host(clusterConfig.getHost())
                .port(clusterConfig.getPort())
                .state("STARTING")
                .registeredAt(Instant.now())
                .lastHeartbeat(Instant.now())
                .currentJobs(0)
                .maxJobs(10)
                .build();

        // Register this node
        registerNode();
    }

    /**
     * Generate a unique node ID.
     */
    private String generateNodeId() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "node";
        }
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Register this node with the cluster.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerNode() {
        try {
            String nodeId = clusterConfig.getNodeId();
            Optional<ClusterNodeEntity> existing = clusterNodeRepository.findById(nodeId);

            if (existing.isPresent()) {
                // Update existing node
                ClusterNodeEntity node = existing.get();
                node.setState(NodeState.ACTIVE);
                node.setLastHeartbeat(Instant.now());
                node.setHost(clusterConfig.getHost());
                node.setPort(clusterConfig.getPort());
                clusterNodeRepository.save(node);
                log.info("Re-registered existing node: {}", nodeId);
            } else {
                // Create new node
                ClusterNodeEntity newNode = ClusterNodeEntity.builder()
                        .nodeId(nodeId)
                        .host(clusterConfig.getHost())
                        .port(clusterConfig.getPort())
                        .state(NodeState.ACTIVE)
                        .lastHeartbeat(Instant.now())
                        .maxJobs(10)
                        .currentJobs(0)
                        .build();
                clusterNodeRepository.save(newNode);
                log.info("Registered new node: {} at {}:{}", nodeId, clusterConfig.getHost(), clusterConfig.getPort());
            }

            // Update local state
            thisNode.setState("ACTIVE");
            thisNode.setLastHeartbeat(Instant.now());
            nodeCache.put(nodeId, thisNode);

        } catch (Exception e) {
            log.error("Error registering node: {}", e.getMessage(), e);
        }
    }

    /**
     * Deregister this node from the cluster.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deregisterNode() {
        try {
            String nodeId = clusterConfig.getNodeId();
            clusterNodeRepository.updateState(nodeId, NodeState.TERMINATED, Instant.now());
            nodeCache.remove(nodeId);
            log.info("Deregistered node: {}", nodeId);
        } catch (Exception e) {
            log.error("Error deregistering node: {}", e.getMessage(), e);
        }
    }

    /**
     * Send heartbeat to indicate node is alive.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void heartbeat() {
        try {
            String nodeId = clusterConfig.getNodeId();
            int updated = clusterNodeRepository.updateHeartbeat(nodeId, Instant.now());
            if (updated > 0) {
                thisNode.setLastHeartbeat(Instant.now());
                nodeCache.put(nodeId, thisNode);
            }
        } catch (Exception e) {
            log.error("Error sending heartbeat: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all active nodes in the cluster.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClusterNode> getActiveNodes() {
        try {
            Instant threshold = Instant.now().minus(clusterConfig.getHeartbeat().getTimeout());
            List<ClusterNodeEntity> entities = clusterNodeRepository.findActiveNodesWithRecentHeartbeat(threshold);

            List<ClusterNode> nodes = new ArrayList<>();
            for (ClusterNodeEntity entity : entities) {
                ClusterNode node = createClusterNode(entity);
                nodeCache.put(entity.getNodeId(), node);
                nodes.add(node);
            }

            return nodes;
        } catch (Exception e) {
            log.error("Error getting active nodes: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get this node's info.
     */
    public ClusterNode getThisNode() {
        return thisNode;
    }

    /**
     * Check if a node is active.
     */
    public boolean isNodeActive(String nodeId) {
        ClusterNode cached = nodeCache.get(nodeId);
        if (cached != null && cached.getLastHeartbeat() != null) {
            Instant threshold = Instant.now().minus(clusterConfig.getHeartbeat().getTimeout());
            return cached.getLastHeartbeat().isAfter(threshold);
        }

        try {
            Optional<ClusterNodeEntity> entity = clusterNodeRepository.findById(nodeId);
            if (entity.isPresent()) {
                Instant threshold = Instant.now().minus(clusterConfig.getHeartbeat().getTimeout());
                return entity.get().isActive(threshold);
            }
        } catch (Exception e) {
            log.error("Error checking if node {} is active: {}", nodeId, e.getMessage());
        }

        return false;
    }

    /**
     * Get node by ID.
     */
    public Optional<ClusterNode> getNode(String nodeId) {
        // Check cache first
        ClusterNode cached = nodeCache.get(nodeId);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Fetch from database
        try {
            Optional<ClusterNodeEntity> entity = clusterNodeRepository.findById(nodeId);
            if (entity.isPresent()) {
                ClusterNode node = createClusterNode(entity.get());
                nodeCache.put(nodeId, node);
                return Optional.of(node);
            }
        } catch (Exception e) {
            log.error("Error getting node {}: {}", nodeId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Transition node to a new state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transitionNodeState(String nodeId, NodeState newState) {
        try {
            clusterNodeRepository.updateState(nodeId, newState, Instant.now());
            nodeCache.remove(nodeId);
            log.info("Node {} transitioned to state {}", nodeId, newState);
        } catch (Exception e) {
            log.error("Error transitioning node {} to state {}: {}", nodeId, newState, e.getMessage());
        }
    }

    /**
     * Increment job count for this node.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementJobCount() {
        try {
            clusterNodeRepository.incrementJobCount(clusterConfig.getNodeId());
            thisNode.setCurrentJobs(thisNode.getCurrentJobs() + 1);
            nodeCache.put(thisNode.getNodeId(), thisNode);
        } catch (Exception e) {
            log.error("Error incrementing job count: {}", e.getMessage(), e);
        }
    }

    /**
     * Decrement job count for this node.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrementJobCount() {
        try {
            clusterNodeRepository.decrementJobCount(clusterConfig.getNodeId());
            thisNode.setCurrentJobs(Math.max(0, thisNode.getCurrentJobs() - 1));
            nodeCache.put(thisNode.getNodeId(), thisNode);
        } catch (Exception e) {
            log.error("Error decrementing job count: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all nodes that can accept jobs.
     */
    public List<ClusterNode> getNodesThatCanAcceptJobs() {
        try {
            List<ClusterNodeEntity> entities = clusterNodeRepository.findNodesThatCanAcceptJobs();
            List<ClusterNode> nodes = new ArrayList<>();
            for (ClusterNodeEntity entity : entities) {
                nodes.add(createClusterNode(entity));
            }
            return nodes;
        } catch (Exception e) {
            log.error("Error getting nodes that can accept jobs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the count of active nodes.
     */
    public long getActiveNodeCount() {
        try {
            return clusterNodeRepository.countActiveNodes();
        } catch (Exception e) {
            log.error("Error getting active node count: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Convert entity to DTO.
     */
    private ClusterNode createClusterNode(ClusterNodeEntity entity) {
        Map<String, Object> metadata = null;
        if (entity.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(entity.getMetadata(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Could not parse metadata for node {}", entity.getNodeId());
            }
        }

        return ClusterNode.builder()
                .nodeId(entity.getNodeId())
                .host(entity.getHost())
                .port(entity.getPort())
                .state(entity.getState().name())
                .registeredAt(entity.getRegisteredAt())
                .lastHeartbeat(entity.getLastHeartbeat())
                .metadata(metadata)
                .currentJobs(entity.getCurrentJobs())
                .maxJobs(entity.getMaxJobs())
                .version(entity.getVersion())
                .build();
    }

    /**
     * Scheduled task to send heartbeats.
     */
    @Scheduled(fixedDelayString = "${cluster.heartbeat.interval-seconds:10}000")
    @SchedulerLock(name = "ClusterNodeRegistry_scheduledHeartbeat", lockAtMostFor = "PT8S", lockAtLeastFor = "PT3S")
    public void scheduledHeartbeat() {
        if (!clusterConfig.isEnabled()) {
            return;
        }
        heartbeat();
    }

    /**
     * Scheduled task to clean up stale nodes.
     */
    @Scheduled(fixedDelayString = "${cluster.heartbeat.cleanup-interval-seconds:60}000")
    @SchedulerLock(name = "ClusterNodeRegistry_cleanupStaleNodes", lockAtMostFor = "PT48S", lockAtLeastFor = "PT24S")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupStaleNodes() {
        if (!clusterConfig.isEnabled()) {
            return;
        }
        try {
            Instant threshold = Instant.now().minus(clusterConfig.getHeartbeat().getTimeout().multipliedBy(2));
            int cleaned = clusterNodeRepository.cleanupStaleNodes(threshold);

            if (cleaned > 0) {
                log.warn("Cleaned up {} stale nodes", cleaned);
                // Update cache
                for (String nodeId : nodeCache.keySet()) {
                    ClusterNode node = nodeCache.get(nodeId);
                    if (node.getLastHeartbeat() != null && node.getLastHeartbeat().isBefore(threshold)) {
                        nodeCache.remove(nodeId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning up stale nodes: {}", e.getMessage(), e);
        }
    }
}